#!/usr/bin/perl

use strict;
use HTTP::Daemon;
use HTTP::Status;
use HTTP::Response;
use HTTP::Headers;
use HTTP::Message;
use JSON;

print "IMReady Server starting\n";

my $running = 1;
my %meetings = ();
my $latestKey = 1;
  
while($running){
    my $daemon = HTTP::Daemon->new( LocalPort => 54321 ) || die "Oops - Failed to even start a server.";

    while (my $connection = $daemon->accept) {
        while (my $request = $connection->get_request) {
            if ( $request->uri->path eq '/meetings' ){
                if ( $request->method eq 'GET' ){
                    returnMeetings($connection);
                } elsif ( $request->method eq 'POST' ) {
                    createMeeting($connection, $request);
                }
            } else {
                $connection->send_error();
            }
        }
    }
}

sub returnMeetings {
    my $conn = shift;

    my $hdr     = HTTP::Headers->new(Content_Type => 'text/html',
                                     Connection   => 'close');
    my $content = "<html><body><p>Hello World</p></body></html>";
    
    my $resp = HTTP::Response->new(RC_OK, "", $hdr, $content);
    $conn->send_response($resp);
}

sub createMeeting {
    my $conn = shift;
    my $req  = shift;

    my $hdr     = HTTP::Headers->new(Content_Type => 'application/json',
                                     Connection   => 'close');
    #my $content = to_json($id);
    my $content = "{\"id\":\"1\"}";

    my $resp = HTTP::Response->new(RC_OK, "", $hdr, $content);
    $conn->send_response($resp);
}