#!/bin/bash
#
# A dummy script to act as a stand-in for the zoomify processor during testing.
#
# The first argument is expected to be -Z, the second -o, the third the output file name.  The fourth argument must
# be the input file name.
#

if [ 4 -ne $# ]
then
    echo "Wrong number of arguments.  4 expected."  1>&2
    exit 1
fi

if [ "-Z" != "$1" ]
then
    echo "Invalid first parameter $1, -Z expected."  1>&2
    exit 1
fi

if [ "-o" != "$2" ]
then
    echo "Invalid second parameter $2, -o expected."  1>&2
    exit 1
fi

outfile=$3

if [ -e $4 ]
then
    infile=$4
else
    echo "Invalid input file $4"  1>&2
    exit 1
fi

sha1sum $infile > $outfile
exit $?
