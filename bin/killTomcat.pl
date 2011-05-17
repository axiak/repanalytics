#!/usr/bin/perl
use strict;
use warnings;


sub main () {
    help() unless ($ARGV[1]);
    my ($app_base, $instance) = @ARGV;

    my $ps_line = `ps aux | grep "$app_base" | grep "$instance" | grep java | grep -v grep`;

    my @tokens = split(/\W+/, $ps_line);

    my $pid = $tokens[1];

    exit(0) unless ($pid);

    my $tries = 0;
    while (++$tries < 11 and process_running($pid)) {
        if ($tries > 8) {
            kill(9, $pid);
        }
        elsif ($tries > 7) {
            kill(8, $pid);
        }
        else {
            kill(2, $pid);
        }
        sleep(1);
    }
    exit(process_running($pid));
}


sub process_running ($) {
    my $pid = shift;

    return kill(0, $pid);
}

sub help () {
    print "Kill Tomcat - Meant to be used by ant.\n";
    print "Usage: $0 APP_BASE INSTANCE_NAME\n";
    exit(1);
}

main();
