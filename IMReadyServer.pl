#!/usr/bin/perl

use strict;
use HTTP::Daemon;
use HTTP::Status;
use HTTP::Response;
use HTTP::Headers;
use HTTP::Message;
use CGI;
use Redis;

my ($dbName, $dbUser, $dbPass) = readConfig();

print "IMReady Server starting\n";

my $unamePattern = "[A-Za-z_0-9]+";

my $running = 1;
my %meetings = ();
my $latestKey = 1;
my $listenerPort = 54321;

# Let's connect to a db
use DBI;

main();

sub main{
    debug('Starting server on port <' . $listenerPort . '>');
    while($running){
        my $daemon = HTTP::Daemon->new( LocalPort => $listenerPort, Timeout => 15 ) || die "Oops - Failed to even start a server.";

        while (my $connection = $daemon->accept) {
            debug('Accepted connection');
            if (my $request = $connection->get_request) {
                debug('Request method <' . $request->method . '> to URL <' . $request->uri->path . '>');
                if ( $request->method eq 'GET' ){
                    if ( $request->uri->path =~ /^\/user\/.*$/ ) {
                        if ( $request->uri->path =~ /^\/user\/($unamePattern)$/ ) {
                            my $username = $1;
                            debug('Return user <' . $username . '>');
                            returnUser($connection, $request, $username);
                        } else {
                            debug('Invalid user request');
                            $connection->send_error(404, 'Invalid user');
                        }
                    } elsif ( $request->uri->path eq '/meetings' ) {
                        debug('Return meetings');
                        returnMeetings($connection, $request);
                    } elsif ( $request->uri->path =~ /^\/meetings\/($unamePattern)$/ ) {
                        my $username = $1;
                        debug('Return meetings for user <' . $username . '>');
                        returnUserMeetings($connection, $request, $username);
                    } elsif ( $request->uri->path =~ /^\/meeting\/(\d+)$/ ) {
                        my $meeting = $1;
                        debug('Return meeting <' . $meeting . '>');
                        returnMeeting($connection, $request, $meeting);
                    } elsif ( $request->uri->path =~ /^\/clear\/(\d+)$/ ) { # TODO - add a proper expiry/clearout API
                        # Clear anything that hasn't been touched in 2 weeks
                        # 2 weeks * 7 days * 24 hours * 60 minutes * 60 seconds = 1209600 seconds
                        clearOldMeetings($1);
                    } else {
                        $connection->send_error(500, 'Internal error');
                    }
                } elsif ( $request->method eq 'POST' ) {
                    if ( $request->uri->path eq '/users' ) {
                        debug('Create user');
                        createUser($connection, $request);
                    } elsif ( $request->uri->path eq '/meetings' ) {
                        debug('Create meeting');
                        createMeeting($connection, $request);
                    } elsif ($request->uri->path =~ /^\/meeting\/(\d+)\/participants$/ ) {
                        my $meeting = $1;
                        debug('Adding user to meeting <' . $meeting . '>');
                        addParticipant($connection, $request, $meeting);
                    } else {
                        $connection->send_error(500, 'Internal error');
                    }
                } elsif ( $request->method eq 'PUT' ) {
                    if ( $request->uri->path =~ /^\/meeting\/(\d+)\/participant\/($unamePattern)$/ ) {
                        my $meeting = $1;
                        my $username = $2;
                        debug('Setting the status of user <' . $username . '> in meeting <' . $meeting . '>');
                        setParticipantStatus($connection, $request, $meeting, $username);
                    } else {
                        $connection->send_error(500, 'Internal error');
                    }
                } elsif ( $request->method eq 'DELETE' ) {
                    if ( $request->uri->path =~ /^\/meeting\/(\d+)\/participant\/($unamePattern)$/ ) {
                        my $meeting = $1;
                        my $username = $2;
                        debug('Removing user <' . $username . '> from meeting <' . $meeting . '>');
                        removeParticipant($connection, $request, $meeting, $username);
                    } else {
                        $connection->send_error(500, 'Internal error');
                    }
                } else {
                    $connection->send_error(500, 'Internal error');
                }
            } else {
                debug('Invalid connection');
            }
            debug('Done with connection');
            $connection->close;
            undef($connection);
        }
    }
}

sub returnUser {
    my $conn = shift;
    my $req  = shift;
    my $id   = shift;
    debug('Look-up user <' . $id . '>');

    my $valid = userValid($id);
    if ( $valid == 0 ) {
        debug('Invalid user id <' . $id . '>');
        $conn->send_error(500, "Internal Error");
        return;
    }

    if( userExists( $id ) ) {
        my $content = "{\"username\":\"" . $id . "\", \"name\":\"" . getNickname($id) . "\"}";

        my $hdr     = HTTP::Headers->new( Content_Type => 'application/json',
                                          Connection   => 'close');
        my $resp = HTTP::Response->new(RC_OK, "", $hdr, $content);
        $conn->send_response($resp);
    } else {
        debug('User id not found');
        $conn->send_error(404, "Participant not found");
    }
}

sub returnMeetings {
    my $conn = shift;
    my $req  = shift;

    my $content;

    my $redis;
    unless ( $redis = Redis->new( server => 'localhost:6379', encoding => undef ) ) {
        debug('Failed to connect to Redis');
        $conn->send_error(500, 'Internal error');
        return;
    }

    # If there are meetings defined, build a JSON array, else, return an array of null.
    my @meetings = $redis->smembers("util:list:meetings");
    if( defined $meetings[0] ){
        $content  = "[";
        foreach my $meeting (sort @meetings){
            $content .= "{\"id\": " . $meeting . ", \"name\": \"" . $redis->get("meeting:$meeting:name") . "\"},"; # FIXME state?
        }
        chop $content;
        $content .= "]";
    } else {
        $content  = "[]";
    }
    $redis->quit;

    my $hdr     = HTTP::Headers->new(Content_Type => 'application/json',
                                     Connection   => 'close');
    my $resp = HTTP::Response->new(RC_OK, "", $hdr, $content);
    $conn->send_response($resp);
}

sub returnUserMeetings {
    my $conn = shift;
    my $req  = shift;
    my $id   = shift;

    my $requestingUserID = $req->header('X-IMReady-Auth-ID');
    if( $requestingUserID ) {
        debug('Request came from user <' . $requestingUserID . '>' );

        my $valid = userValid($requestingUserID);
        if ( $valid == 0 ) {
            debug('Invalid user id <' . $requestingUserID . '>');
            $conn->send_error(500, "Internal Error");
            return;
        }

        my $exists = userExists($requestingUserID);
        if ( $exists == 0 ) {
            debug('User id <' . $requestingUserID . '> not found');
            $conn->send_error(404, "User id not found");
            return;
        } elsif ( $exists == -1 ) {
            $conn->send_error(500, "Internal error");
            return;
        }
    }
    
    my $content;

    my $valid = userValid($id);
    if ( $valid == 0 ) {
        debug('Invalid user id <' . $id . '>');
        $conn->send_error(500, "Internal Error");
        return;
    }

    my $exists = userExists($id);
    if ( $exists == 0 ) {
        debug('User id <' . $id . '> not found');
        $conn->send_error(404, "User id not found");
        return;
    } elsif ( $exists == -1 ) {
        $conn->send_error(500, "Internal error");
        return;
    }

    my $redis;
    unless ( $redis = Redis->new( server => 'localhost:6379', encoding => undef ) ) {
        debug('Failed to connect to Redis');
        $conn->send_error(500, 'Internal error');
        return;
    }

    # If there are meetings for this user, build a JSON array, else, return an array of null.
    my @meetings = $redis->smembers("user:$id:meetings");
    if( defined $meetings[0] ){
        $content  = "[";
        foreach my $meeting (sort @meetings){
            $content .= "{\"id\": " . $meeting . ",";
            $content .= " \"name\": \"" . $redis->get("meeting:$meeting:name") . "\",";
            $content .= " \"state\": " . $redis->get("meeting:$meeting:state") . ",";
            if( $requestingUserID && $requestingUserID eq $id ) {
                debug('Setting user <' . $id . '> in meeting <' . $meeting . '> notified to 1');
                $redis->set("meeting:$meeting:$id:notified", 1);
                $redis->set("meeting:$meeting:touched" => time); 
                # TODO: Decide when a meeting counts as being touched, atm this is probably inconsistent with returnMeeting()
            }
            my @participants = $redis->smembers("meeting:$meeting:participants");
            $content .= " \"participants\": [";
            foreach my $participant (sort @participants){
                if( $requestingUserID && $requestingUserID eq $participant ) {
                    debug('Setting user <' . $participant . '> in meeting <' . $meeting . '> notified to 1');
                    $redis->set("meeting:$meeting:$participant:notified", 1);
                }
                $content .= "{\"id\": \"" . $participant . "\",";
                $content .= " \"defaultNickname\": \"" . getNickname($participant) . "\",";
                $content .= " \"state\": " . $redis->get("meeting:$meeting:$participant:state") . ",";
                $content .= " \"notified\": " . ($redis->get("meeting:$meeting:$participant:notified") ? "true" : "false") . " },";
            }
            chop $content;
            $content .= "]";
            $content .= "},";
        }
        chop $content;
        $content .= "]";
    } else {
        $content  = "[]";
    }
    $redis->quit;

    my $hdr     = HTTP::Headers->new(Content_Type => 'application/json',
                                     Connection   => 'close');
    my $resp = HTTP::Response->new(RC_OK, "", $hdr, $content);
    $conn->send_response($resp);
}

sub returnMeeting {
    my $conn    = shift;
    my $req     = shift;
    my $meeting = shift;

    my $requestingUserID = getRequestingUseID($conn, $req);
    if ( $requestingUserID == -1 ) {
        return;
    }

    my $redis;
    unless ( $redis = Redis->new( server => 'localhost:6379', encoding => undef ) ) {
        debug('Failed to connect to Redis');
        $conn->send_error(500, 'Internal error');
        return;
    }

    my $content;
    # if meeting exists, return information
    if ( $redis->sismember("util:list:meetings", $meeting) ) {
        $content  = "{\"id\": " . $meeting . ",";
        $content .= " \"name\": \"" . $redis->get("meeting:$meeting:name") . "\",";
        $content .= " \"state\": " . $redis->get("meeting:$meeting:state") . ",";
        $redis->set("meeting:$meeting:touched" => time);
        # $content = Add state
        my @participants = $redis->smembers("meeting:$meeting:participants");
        $content .= " \"participants\": [";
        foreach my $participant (sort @participants){
            if( $requestingUserID && $requestingUserID eq $participant ) {
                debug('Setting user <' . $participant . '> in meeting <' . $meeting . '> notified to 1');
                $redis->set("meeting:$meeting:$participant:notified", 1);
            }
            $content .= "{\"id\": \"" . $participant . "\",";
            $content .= " \"defaultNickname\": \"" . getNickname($participant) . "\",";
            $content .= " \"state\": " . $redis->get("meeting:$meeting:$participant:state") . ",";
            $content .= " \"notified\": " . ($redis->get("meeting:$meeting:$participant:notified") ? "true" : "false") . " },";
        }
        chop $content;
        $content .= "]";
        $content .= "}";
    } else {
        $content  = "{\"id\": null}";
    }
    $redis->quit;

    my $hdr     = HTTP::Headers->new(Content_Type => 'application/json',
                                     Connection   => 'close');
    my $resp = HTTP::Response->new(RC_OK, "", $hdr, $content);
    $conn->send_response($resp);
}

sub createUser {
    my $conn = shift;
    my $req = shift;

    my $q = CGI->new($req->content);
    my $id = $q->param('id');
    my $nickname = $q->param('defaultNickname');
    debug('Look-up user <' . $id . "> with name <" . $nickname . ">" );

    # Check the user id is valid
    my $valid = userValid($id);
    if ( $valid == 0 ) {
        debug('Invalid user id <' . $id . '>');
        $conn->send_error(500, "Internal Error");
        return;
    }

    if( userExists($id) ){
        debug('User id already taken');
        $conn->send_error(400, 'User id already taken');
        return;
    }

    my $db;
    unless ( $db = DBI->connect("DBI:mysql:$dbName", $dbUser, $dbPass) ){
        debug('Failed to connect to database');
        $conn->send_error(500, 'Internal error');
        return;
    }

    debug('Create user');
    my $s = $db->prepare("INSERT INTO users (name,username) VALUES (?,?)");
    my $rows = $s->execute($nickname, $id);
    if ($rows == 1) {
        debug('Success');
        my $content = "{\"username\":\"$id\", \"name\":\"$nickname\"}";

        my $redis;
        if ( $redis = Redis->new( server => 'localhost:6379', encoding => undef ) ) {
            $redis->set( "user:$id:nickname", $nickname );
            $redis->expire( "user:$id:nickname", 100000 );
        } else {
            debug('Failed to connect to Redis.  Never mind.');
        }

        my $hdr     = HTTP::Headers->new(Content_Type => 'application/json',
                                             Connection   => 'close');
        my $resp = HTTP::Response->new(RC_OK, "", $hdr, $content);
        $conn->send_response($resp);
    } else {
        debug('Failure, $rows rows inserted');
        $conn->send_error(500, "Internal error");
    }

    $s->finish();
    $db->disconnect();
}

sub createMeeting {
    my $conn  = shift;
    my $req   = shift;

    my $q = CGI->new($req->content);
    my $id = $q->param('creator');
    my $meetingName = $q->param('name');
    debug('Asked to make meeting for <' . $id . '> called <' . $meetingName . '>');

    my $valid = userValid($id);
    if ( $valid == 0 ) {
        debug('Invalid user id <' . $id . '>');
        $conn->send_error(500, "Internal Error");
        return;
    }

    my $exists = userExists($id);
    if ( $exists == 0 ) {
        debug('User id <' . $id . '> not found');
        $conn->send_error(404, "User id not found");
        return;
    } elsif ( $exists == -1 ) {
        $conn->send_error(500, "Internal Error");
        return;
    }

    my $redis;
    unless ( $redis = Redis->new( server => 'localhost:6379', encoding => undef ) ) {
        debug('Failed to connect to Redis');
        $conn->send_error(500, 'Internal error');
        return;
    }

    # Create the Redis keys for the meeting
    my $meeting = $redis->incr('util:counter:meetings');
    $redis->set("meeting:$meeting:name" => $meetingName);
    $redis->set("meeting:$meeting:state" => 0);
    $redis->set("meeting:$meeting:touched" => time);
    $redis->sadd("meeting:$meeting:participants" => $id);
    $redis->set("meeting:$meeting:$id:state" => 0);
    $redis->set("meeting:$meeting:$id:notified" => 1);
    $redis->sadd("util:list:meetings" => $meeting);
    $redis->sadd("user:$id:meetings" => $meeting);
    $redis->quit;

    debug('meeting made with id <' . $meeting . '>');

    my $hdr     = HTTP::Headers->new(Content_Type => 'application/json',
                                     Connection   => 'close');
    my $content = "{\"id\":\"$meeting\"}";

    my $resp = HTTP::Response->new(RC_OK, "", $hdr, $content);
    $conn->send_response($resp);
}

sub addParticipant {
    my $conn    = shift;
    my $req     = shift;
    my $meeting = shift;
    
    my $q = CGI->new($req->content);
    my $id = $q->param('id');
    debug('Add user <' . $id . "> to meeting <" . $meeting . ">" );

    # Check the user id is valid
    my $valid = userValid($id);
    if ( $valid == 0 ) {
        debug('Invalid user id <' . $id . '>');
        $conn->send_error(500, "Internal Error");
        return;
    }

    my $exists = userExists($id);
    if ( $exists == 0 ) {
        debug('User id <' . $id . '> not found');
        $conn->send_error(404, "User id not found");
        return;
    } elsif ( $exists == -1 ) {
        $conn->send_error(500, "Internal error");
        return;
    }

    my $redis;
    unless ( $redis = Redis->new( server => 'localhost:6379', encoding => undef ) ) {
        debug('Failed to connect to Redis');
        $conn->send_error(500, "Internal error");
        return;
    }

    unless ( $redis->sismember("util:list:meetings", $meeting) ) {
        debug('Meeting id <' . $meeting . '> not found');
        $conn->send_error(404, "Meeting not found");
        return;
    }

    $redis->sadd("meeting:$meeting:participants" => $id);
    $redis->set("meeting:$meeting:$id:state" => 0);
    $redis->set("meeting:$meeting:$id:notified" => 0);
    $redis->sadd("user:$id:meetings" => $meeting);
    $redis->set("meeting:$meeting:touched" => time);
    $redis->quit;

    my $hdr     = HTTP::Headers->new(Content_Type => 'application/json',
                                     Connection   => 'close');
    my $content = "";

    my $resp = HTTP::Response->new(RC_OK, "", $hdr, $content);
    $conn->send_response($resp);
}

sub removeParticipant {
    my $conn    = shift;
    my $req     = shift;
    my $meeting = shift;
    my $id      = shift;
    
    debug('Remove user <' . $id . "> from meeting <" . $meeting . ">" );

    # Check the user id is valid
    my $valid = userValid($id);
    if ( $valid == 0 ) {
        debug('Invalid user id <' . $id . '>');
        $conn->send_error(500, "Internal Error");
        return;
    }

    my $exists = userExists($id);
    if ( $exists == 0 ) {
        debug('User id <' . $id . '> not found');
        $conn->send_error(404, "User id not found");
        return;
    } elsif ( $exists == -1 ) {
        $conn->send_error(500, "Internal error");
        return;
    }

    my $redis;
    unless ( $redis = Redis->new( server => 'localhost:6379', encoding => undef ) ) {
        debug('Failed to connect to Redis');
        $conn->send_error(500, "Internal error");
        return;
    }

    unless ( $redis->sismember("util:list:meetings", $meeting) ) {
        debug('Meeting id <' . $meeting . '> not found');
        $conn->send_error(404, "Meeting not found");
        return;
    }
    unless ( $redis->sismember("meeting:$meeting:participants", $id) ) {
        debug('User id <' . $id . '> not a participant of meeting <' . $meeting . '>');
        $conn->send_error(400, "User not a participant of meeting");
        return;
    }

    $redis->srem("meeting:$meeting:participants" => $id);
    $redis->del("meeting:$meeting:$id:state");
    $redis->del("meeting:$meeting:$id:notified");
    $redis->srem("user:$id:meetings" => $meeting);
    $redis->set("meeting:$meeting:touched" => time);
    $redis->quit;

    my $hdr     = HTTP::Headers->new(Content_Type => 'application/json',
                                     Connection   => 'close');
    my $content = "";

    my $resp = HTTP::Response->new(RC_OK, "", $hdr, $content);
    $conn->send_response($resp);
}

sub setParticipantStatus {
    my $conn    = shift;
    my $req     = shift;
    my $meeting = shift;
    my $id      = shift;

    my $q = CGI->new($req->content);
    my $state = $q->param('status');
    debug( 'Setting participant <' . $id . '> in meeting <' . $meeting . '> to state <' . $state . '>' );

    # Check the user id is valid
    my $valid = userValid($id);
    if ( $valid == 0 ) {
        debug('Invalid user id <' . $id . '>');
        $conn->send_error(500, "Internal Error");
        return;
    }

    # Check the userid exists
    my $exists = userExists($id);
    if ( $exists == 0 ) {
        debug('User id <' . $id . '> not found');
        $conn->send_error(404, "User id not found");
        return;
    } elsif ( $exists == -1 ) {
        $conn->send_error(500, "Internal Error");
        return;
    }

    # Set the participant's status
    my $redis;
    if ( $redis = Redis->new( server => 'localhost:6379', encoding => undef ) ) {
        $redis->set("meeting:$meeting:touched" => time);
        if ( $state eq 'ready' ) {
            $redis->set( "meeting:$meeting:$id:state" => 1);
        } else {
            debug( 'failed to set user status' );
            $conn->send_error(500, "Internal error");
        }
        # Check if the meeting is now ready
        if( $redis->get("meeting:$meeting:state") eq 0 ){
            debug( 'checking if the meeting is now ready' );
            my @participants = $redis->smembers("meeting:$meeting:participants");
            if ( scalar @participants > 0 ) {
                my $meetingState = 1;
                foreach my $participant (sort @participants){
                    if( $redis->get("meeting:$meeting:$participant:state") eq 0 ){
                        debug( 'meeting is not ready as we are still waiting for participant <' . $participant . '>' );
                        $meetingState = 0;
                    }
                }
                $redis->set("meeting:$meeting:state" => $meetingState);
            }
        }
    } else {
        debug( 'Failed to connect to Redis' );
        $conn->send_error(500, "Internal error");
    }

    my $hdr     = HTTP::Headers->new(Content_Type => 'application/json',
                                     Connection   => 'close');
    my $content = "";

    my $resp = HTTP::Response->new(RC_OK, "", $hdr, $content);
    $conn->send_response($resp);
}

# Return the userID of the user making this request.
# Check for an X-IMReady-Auth-ID header
# Return -1 on an error.
sub getRequestingUseID {
    my $conn = shift;
    my $req  = shift;

    my $requestingUserID = $req->header('X-IMReady-Auth-ID');
    if( $requestingUserID ) {
        debug('Request came from user <' . $requestingUserID . '>' );

        my $valid = userValid($requestingUserID);
        if ( $valid == 0 ) {
            debug('Invalid user id <' . $requestingUserID . '>');
            $conn->send_error(500, "Internal Error");
            return -1;
        }

        my $exists = userExists($requestingUserID);
        if ( $exists == 0 ) {
            debug('User id <' . $requestingUserID . '> not found');
            $conn->send_error(404, "User id not found");
            return -1;
        } elsif ( $exists == -1 ) {
            $conn->send_error(500, "Internal error");
            return -1;
        } else {
            return $requestingUserID;
        }
    } else {
        return;
    }
}

# Go through all meetings and clear up any old meetings
sub clearOldMeetings {
    my $lastday  = shift;
    my $deadline = time - $lastday;
    debug( 'Removing any meetings that have not been touched since <' . $deadline . '>' );

    my $redis;
    if ( $redis = Redis->new( server => 'localhost:6379', encoding => undef ) ) {
        my @meetings = $redis->smembers("util:list:meetings");
        if( defined $meetings[0] ){
            foreach my $meeting (sort @meetings){
                if( $redis->get("meeting:$meeting:touched") <= $deadline ) {

                    my @participants = $redis->smembers("meeting:$meeting:participants");
                    if ( scalar @participants > 0 ) {
                        foreach my $participant (sort @participants){
                            $redis->srem("user:$participant:meetings" => $meeting);
                            $redis->srem("meeting:$meeting:participants" => $participant);
                            $redis->del("meeting:$meeting:$participant:state");
                            $redis->del("meeting:$meeting:$participant:notified");
                            if ( scalar @participants == 1 ) {
                                $redis->del("user:$participant:meetings");
                            }
                        }
                    }
                    $redis->srem("util:list:meetings" => $meeting);
                    $redis->del("meeting:$meeting:name");
                    $redis->del("meeting:$meeting:state");
                    $redis->del("meeting:$meeting:touched");
                    $redis->del("meeting:$meeting:participants");
                }
            }
        }

        # TODO - Validity check?
        # for each participant
        #  for each of their meetings
        #   check meeting exists
        #   if not, remove from participant's list?  Remove all keys for that meeting? remove from meeting list?
        # Glad this is single threaded or we could delete stuff while someone's making it!
        # God, we need a global lock =(

        $redis->quit;
    }

    return; 
}

# Check if a user is valid
# Return 1 for valid
#        0 for invalid
sub userValid {
    my $id = shift;

    if ( $id =~ /^$unamePattern$/ ) {
        return 1;
    }
    return 0;
}

# Check if a user exists.
# Return 1 for exists
#        0 for doesn't exist
#       -1 for an error
sub userExists {
    my $id = shift;

    # Check Redis first;
    my $redis;
    if ( $redis = Redis->new( server => 'localhost:6379', encoding => undef ) ) {
        if ( $redis->get( "user:$id" ) ) {
            debug( 'Redis found user <' . $id . '>' );
            $redis->expire( "user:$id", 100000);
            return 1;
        }
    } else {
        debug( 'Failed to connect to Redis.  Continue to DB' );
    }

    my $db;
    if ( $db = DBI->connect("DBI:mysql:$dbName", $dbUser, $dbPass) ){
        my $s = $db->prepare("SELECT username FROM users WHERE username=?");
        $s->execute($id);
        my $result = $s->fetchrow_hashref();
        $s->finish();
        $db->disconnect();

        if ($result) {
            debug( 'Database found user <' . $id . '>' );
            if ( $redis = Redis->new( server => 'localhost:6379', encoding => undef ) ) {
                $redis->set( "user:$id", 1);
                $redis->expire( "user:$id", 100000);
            } else {
                debug( 'Failed to connect to Redis.  User flag not cached' );
            }
            return 1;
        } else {
            return 0;
        }
    } else {
        debug('Failed to connect to database');
        return -1;
    }
}

sub getNickname {
    my $id = shift;
    
    # Check Redis first;
    my $redis;
    if ( $redis = Redis->new( server => 'localhost:6379', encoding => undef ) ) {
        my $nickname = $redis->get("user:$id:nickname");
        if ( $nickname ) {
            debug( 'Redis found user <' . $id . '> with nickname <' . $nickname . '>' );
            $redis->expire( "user:$id:nickname", 100000 );
            return $nickname;
        }
    } else {
        debug( 'Failed to connect to Redis.  continue to DB' );
    }

    # Then check the DB
    my $db;
    if ( $db = DBI->connect("DBI:mysql:$dbName", $dbUser, $dbPass) ){
        my $s = $db->prepare("SELECT name FROM users WHERE username=?");
        $s->execute($id);
        my $result = $s->fetchrow_hashref();
        $s->finish();
        $db->disconnect();
    
        if ($result) {
        	my $nickname = $result->{'name'};
            debug( 'Database found user <' . $id . '> with nickname <' . $nickname . '>' );
            if ( $redis = Redis->new( server => 'localhost:6379', encoding => undef ) ) {
                $redis->set( "user:$id:nickname", $nickname );
                $redis->expire( "user:$id:nickname", 100000 );
                $redis->set( "user:$id", 1);
                $redis->expire( "user:$id", 100000);
            } else {
                debug( 'Failed to connect to Redis.  User nickname not cached' );
            }
            return $nickname;
        } else {
            return;
        }
    } else {
        debug( 'Failed to connect to database' );
        return;
    }
}

sub debug {
    my $msg = shift;
    my $timestamp = localtime(time);
    print STDERR $timestamp . " : " . (caller(1))[3] . " : " . $msg;
    print STDERR "\n";
}

sub readConfig {
    open(my $f, '<', 'credentials') || die "Failed to get credentials: $!";
    my $dbnm = readline($f); chomp($dbnm);
    my $user = readline($f); chomp($user);
    my $pass = readline($f); chomp($pass);
    close($f);
    return ($dbnm, $user, $pass);
}
