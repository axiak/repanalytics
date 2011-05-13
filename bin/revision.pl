#!/usr/bin/perl
use strict;
use warnings;
use FindBin;

our $LINE_BREAK = "\n";

$LINE_BREAK = "\r\n"
  if ($^O =~ /Win/);

sub main () {
    chdir "$FindBin::Bin/../";
    my $printed = 0;
    if (-e "./.svn") {
        open(my $f, 'svn info |');
        while (<$f>) {
            if (/^Revision:\s*(\d+)/) {
                $printed = 1;
                print "$1$LINE_BREAK";
                while (<$f>) {}
                exit(0);
            }
        }
        print STDERR uc("Using fallback mechanism to determine svn revision. Please install svn in PATH for more robust way.$LINE_BREAK");
        open($f, ".svn/entries") or die("Could not open entries file. Try installing subversion");
        my $line = 0;
        while (<$f>) {
            if ($line == 3) {
                if (/^(\d+)$/) {
                    print "$1$LINE_BREAK";
                    exit(0);
                }
                last;
            }
            $line++;
        }
        close($f);

    } else {
        open(my $f, 'git log |') or die("Could not open git. Is the executable on the path?");
        while (<$f>) {
            if (/git-svn-id: .*?\@(\d+)/) {
                $printed = 1;
                print "$1$LINE_BREAK";
                exit(0);
            }
        }
        close($f);
    }
    if (!$printed) {
        print STDERR "Could not get revision. Do you have svn in the path?$LINE_BREAK";
        exit(1);
    }
}

main();
