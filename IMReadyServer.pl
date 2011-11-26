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
        my $daemon = HTTP::Daemon->new( LocalPort => $listenerPort ) || die "Oops - Failed to even start a server.";
    
        while (my $connection = $daemon->accept) {
            debug('Accepted connection');
            if (my $request = $connection->get_request) {
                debug('Request method <' . $request->method . '> to URL <' . $request->uri->path . '>');
                if ( $request->method eq 'GET' ){
                    if ( $request->uri->path =~ /^\/user\/.*$/ ) {
                        if ( $request->uri->path =~ /^\/user\/($unamePattern)$/ ) {
                            my $username = $1;
                            debug('Return user <' . $username . '>');
                            returnUser($connection, $username);
                        } else {
                            debug('Invalid user request');
                            $connection->send_error(404, 'Invalid user');
                        }
                    } elsif ( $request->uri->path eq '/meetings' ) {
                        debug('Return meetings');
                        returnMeetings($connection);
                    } elsif ($request->uri->path =~ /^\/meeting\/(\d+)$/ ) {
                        my $meeting = $1;
                        debug('Return meeting <' . $meeting . '>');
                        returnMeeting($connection, $meeting);
                    } else {
                        $connection->send_error(500, 'Internal error');
                    }
                } elsif ( $request->method eq 'POST' ) {
                    if ( $request->uri->path eq '/users' ) {
                        debug('Create participant');
                        createUser($connection, $request);
                    } elsif ( $request->uri->path eq '/meetings' ) {
                        debug('Create meeting');
                        createMeeting($connection, $request);
                    } else {
                        $connection->send_error(500, 'Internal error');
                    }
                } elsif ( $request->method eq 'PUT' ) {
                    if ($request->uri->path =~ /^\/meeting\/(\d+)\/participants$/ ) {
                        my $meeting = $1;
                        debug('Adding user to meeting <' . $meeting . '>');
                        addParticipant($connection, $request, $meeting);
                    } elsif ( $request->uri->path =~ /^\/meeting\/(\d+)\/participant\/($unamePattern)$/ ) {
                        my $meeting = $1;
                        my $username = $2;
                        debug('Setting the status of user <' . $username . '> in meeting <' . $meeting . '>');
                        setParticipantStatus($connection, $request, $meeting, $username);
                    } else {
                        $connection->send_error(500, 'Internal error');
                    }
                }
            }
            $connection->close;
            undef($connection);
        }
    }
}

sub returnMeetings {
    my $conn = shift;

    my $content;

    my $redis;
    unless ( $redis = Redis->new( server => 'localhost:6379', encoding => undef ) ) {
        debug('Failed to connect to Redis');
        $conn->send_error(500, 'Internal error');
        return;
    }

    # If there are meetings defined, build a JSON array, else, return an array of null.
    my @meetings = $redis->smembers("util:list:meetings");
    if( scalar @meetings > 0 ){
        $content  = "[";
        foreach my $meeting (sort @meetings){
            $content .= "{\"id\": \"" . $meeting . "\", \"name\": \"" . $redis->get("meeting:$meeting:name") . "\"},";
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
    my $meeting = shift;

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
        # $content = Add state
        my @participants = $redis->smembers("meeting:$meeting:participants");
        if ( scalar @participants > 0 ) {
            $content .= " \"participants\": [";
            foreach my $participant (sort @participants){
                $content .= "{\"id\": \"" . $participant .
                            "\", \"defaultNickname\": \"" . getNickname($participant) .
                            "\", \"state\": \"" . $redis->get("meeting:$meeting:$participant:state") .
                            "\", \"notified\": \"" . $redis->get("meeting:$meeting:$participant:notified") . "\" },";
            }
            chop $content;
            $content .= "]";
        } else {
            $content .= " \"participants\": null";            
        }
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

sub createMeeting {
    my $conn  = shift;
    my $req   = shift;

    my $q = CGI->new($req->content);
    my $id = $q->param('creator');
    my $meetingName = $q->param('name');
    debug('Asked to make meeting for <' . $id . '> called <' . $meetingName . '>');

    my $exists = userExists($id);
    if ( $exists == 0 ) {
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
    $redis->sadd("meeting:$meeting:participants" => $id);
    $redis->set("meeting:$meeting:$id:state" => 0);
    $redis->set("meeting:$meeting:$id:notified" => 1);
    $redis->sadd("util:list:meetings" => $id);
    $redis->sadd("user:$id:meetings" => $id);
    $redis->quit;

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

    my $exists = userExists($id);
    if ( $exists == 0 ) {
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
    $redis->sadd("meeting:$meeting:participants" => $id);
    $redis->set("meeting:$meeting:$id:state" => 0);
    $redis->set("meeting:$meeting:$id:notified" => 0);
    $redis->sadd("user:$id:meetings" => $meeting);
    $redis->quit;

    my $hdr     = HTTP::Headers->new(Content_Type => 'application/json',
                                     Connection   => 'close');
    my $content = "";

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
    if ( $id !~ /$unamePattern/ ) {
        debug('Invalid user id');
        $conn->send_error(400, 'Invalid user id');
        return;
    }

    my $db;
    unless ( $db = DBI->connect("DBI:mysql:$dbName", $dbUser, $dbPass) ){
        debug('Failed to connect to database');
        $conn->send_error(500, 'Internal error');
        return;
    }

    my $s = $db->prepare("SELECT name,username FROM users WHERE username=?");
    $s->execute($id);
    my $result = $s->fetchrow_hashref();
    if ($result) {
        debug('User already taken');
        $conn->send_error(400, 'User already taken');
    } else {
        debug('Create user');
        $s = $db->prepare("INSERT INTO users (name,username) VALUES (?,?)");
        my $rows = $s->execute($nickname, $id);
        if ($rows == 1) {
            debug('Success');
            my $content = "{\"username\":\"$id\", \"name\":\"$nickname\"}";

            my $redis;
            if ( $redis = Redis->new( server => 'localhost:6379', encoding => undef ) ) {
                $redis->setex( "user:$id:nickname", 100000, $nickname );
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
    }
    $s->finish();
    $db->disconnect();
}

sub returnUser {
    my $conn = shift;
    my $id = shift;
    debug('Look-up user <' . $id . '>');

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

sub setParticipantStatus {
    my $conn    = shift;
    my $req     = shift;
    my $meeting = shift;
    my $id      = shift;
    
    my $q = CGI->new($req->content);
    my $state = $q->param('status');
    debug( 'Setting participant <' . $id . '> in meeting <' . $meeting . '> to state <' . $state . '>' );
    
    my $redis;
    if ( $redis = Redis->new( server => 'localhost:6379', encoding => undef ) ) {
        if ( $state eq 'ready' ) {
            $redis->set( "meeting:$meeting:$id:state" => 1);
        } else {
            debug( 'failed to set user status' );
            $conn->send_error(500, "Internal error");
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

# Check if a user exists.
# Return 1 for exists
#        0 for doesn't exist
#       -1 for an error
sub userExists {
    my $id = shift;

    # Check Redis first;
    my $redis;
    if ( $redis = Redis->new( server => 'localhost:6379', encoding => undef ) ) {
        if ( $redis->sismember( "util:list:users", $id) ) {
            debug( 'Redis found user <' . $id . '>' );
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
                $redis->setex( "user:$id", 100000, 1);
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
            return $nickname;
        }
    } else {
        debug( 'Failed to connect to Redis.  continue to DB' );
    }

    # Then check the DB
    my $db;
    if ( $db = DBI->connect("DBI:mysql:$dbName", $dbUser, $dbPass) ){
        my $s = $db->prepare("SELECT username FROM users WHERE username=?");
        $s->execute($id);
        my $nickname = $s->fetchrow_hashref();
        $s->finish();
        $db->disconnect();
    
        if ($nickname) {
            debug( 'Database found user <' . $id . '> with nickname <' . $nickname . '>' );
            if ( $redis = Redis->new( server => 'localhost:6379', encoding => undef ) ) {
                $redis->setex( "user:$id:nickname", 100000, $nickname );
                $redis->setex( "user:$id", 100000, 1);
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