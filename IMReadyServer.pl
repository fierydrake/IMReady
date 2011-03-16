#!/usr/bin/perl

use strict;
use HTTP::Daemon;
use HTTP::Status;
use HTTP::Response;
use HTTP::Headers;
use HTTP::Message;
use CGI;
#use JSON;

my ($dbName, $dbUser, $dbPass) = readConfig();

print "IMReady Server starting\n";

my $unamePattern = "[A-Za-z_0-9]+";

my $running = 1;
my %meetings = ();
my $latestKey = 1;

# Let's connect to a db
use DBI;
 
while($running){
    my $daemon = HTTP::Daemon->new( LocalPort => 54321 ) || die "Oops - Failed to even start a server.";

    while (my $connection = $daemon->accept) {
        debug('Accepted connection');
        if (my $request = $connection->get_request) {
            my $db = DBI->connect("DBI:mysql:$dbName", $dbUser, $dbPass) || die "No db connect: $DBI::errstr";
            if ( $request->uri->path eq '/meetings' ) {
                if ( $request->method eq 'GET' ){
                    debug('Return meetings');
                    returnMeetings($db, $connection);
                } elsif ( $request->method eq 'POST' ) {
                    debug('Create meeting');
                    createMeeting($db, $connection, $request);
                } else {
                    $connection->send_error();
                }
            } elsif ( $request->uri->path eq '/participants' ) {
                if ( $request->method eq 'POST' ) {
                    debug('Create participant');
                    createParticipant($db, $connection, $request);
                } else {
                    $connection->send_error();
                }
            } elsif ( $request->uri->path =~ /^\/participant\/($unamePattern)$/ ) {
                my $username = $1;
                if ( $request->method eq 'GET' ) {
                    debug('Return participant');
                    returnParticipant($db, $connection, $username);
                } else {
                    $connection->send_error();
                }
            } else {
                $connection->send_error();
            }
            $db->disconnect();
        }
        $connection->close;
        undef($connection);
    }
}

sub returnMeetings {
    my $db = shift;
    my $conn = shift;

    my $hdr     = HTTP::Headers->new(Content_Type => 'text/html',
                                     Connection   => 'close');
    my $content = "<html><body><p>Hello World</p></body></html>";
    
    my $resp = HTTP::Response->new(RC_OK, "", $hdr, $content);
    $conn->send_response($resp);
}

sub createMeeting {
    my $db = shift;
    my $conn = shift;
    my $req  = shift;

    my $hdr     = HTTP::Headers->new(Content_Type => 'application/json',
                                     Connection   => 'close');
    #my $content = to_json($id);
    my $content = "{\"id\":\"1\"}";

    my $resp = HTTP::Response->new(RC_OK, "", $hdr, $content);
    $conn->send_response($resp);
}

sub createParticipant {
    my $db = shift;
    my $conn = shift;
    my $req = shift;

    my $hdr     = HTTP::Headers->new(Content_Type => 'application/json',
                                     Connection   => 'close');
    my $q = CGI->new($req->content);
    my $username = $q->param('username');
    my $name = $q->param('name');

    debug('Look-up user');
    my $s = $db->prepare("SELECT name,username FROM users WHERE username=?");
    $s->execute($username);
    my $result = $s->fetchrow_hashref();
    if ($result) {
        debug('User already taken');
        $conn->send_error();
    } else {
        debug('Create user');
        $s = $db->prepare("INSERT INTO users (name,username) VALUES (?,?)");
        my $rows = $s->execute($name, $username);
        if ($rows == 1) {
            debug("Success");
            my $content = "{\"username\":\"$username\", \"name\":\"$name\"}";

            my $resp = HTTP::Response->new(RC_OK, "", $hdr, $content);
            $conn->send_response($resp);
        } else {
            debug("Failure, $rows rows inserted");
            $conn->send_error(500, "Failed to process");
        }
    }
}

sub returnParticipant {
    my $db = shift;
    my $conn = shift;
    my $username = shift;

    my $hdr     = HTTP::Headers->new(Content_Type => 'application/json',
                                     Connection   => 'close');
    
    debug('Look-up user');
    my $s = $db->prepare("SELECT name,username FROM users WHERE username=?");
    $s->execute($username);
    my $result = $s->fetchrow_hashref();
    if ($result) {
        debug('User found, send result');
        # TODO Marshall JSON properly
        my $name = $result->{'name'};
        my $content = qq({"username":"$username", "name":"$name"});

        my $resp = HTTP::Response->new(RC_OK, "", $hdr, $content);
        $conn->send_response($resp);
    } else {
        debug('User not found');
        $conn->send_error(404, "Participant not found");
    }
}

sub debug {
    my $msg = shift;
    my $timestamp = localtime(time);
    print STDERR $timestamp . " : " . $msg;
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
