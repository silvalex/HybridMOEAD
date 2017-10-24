#!/bin/sh
#
# Force Bourne Shell if not Sun Grid Engine default shell (you never know!)
#
#$ -S /bin/sh
#
# I know I have a directory here so I'll use it as my initial working directory
#
#$ -wd /vol/grid-solar/sgeusers/sawczualex
#
# End of the setup directives
#
# Now let's do something useful, but first change into the job-specific directory that should
#  have been created for us
#
# Check we have somewhere to work now and if we don't, exit nicely.
#

if [ -d /local/tmp/sawczualex/$JOB_ID.$SGE_TASK_ID ]; then
        cd /local/tmp/sawczualex/$JOB_ID.$SGE_TASK_ID
else
        echo "Uh oh ! There's no job directory to change into "
        echo "Something is broken. I should inform the programmers"
        echo "Save some information that may be of use to them"
        echo "Here's LOCAL TMP "
        ls -la /local/tmp
        echo "AND LOCAL TMP SAWCZUALEX "
        ls -la /local/tmp/sawczualex
        echo "Exiting"
        exit 1
fi

#
# Now we are in the job-specific directory so now can do something useful
#
# Stdout from programs and shell echos will go into the file
#    scriptname.o$JOB_ID
#  so we'll put a few things in there to help us see what went on
#

echo ==UNAME==
uname -n
echo ==WHO AM I and GROUPS==
id
groups
echo ==SGE_O_WORKDIR==
echo $SGE_O_WORKDIR
echo ==/LOCAL/TMP==
ls -ltr /local/tmp/
echo ==/VOL/GRID-SOLAR==
ls -l /vol/grid-solar/sgeusers/

#
# OK, where are we starting from and what's the environment we're in
#
echo ==RUN HOME==
pwd
ls
echo ==ENV==
env
echo ==SET==
set
#
echo == WHATS IN LOCAL/TMP ON THE MACHINE WE ARE RUNNING ON ==
ls -ltra /local/tmp | tail
#
echo == WHATS IN LOCAL TMP FRED JOB_ID AT THE START==
ls -la

# -----------------------------------

#
# Initialise path variables
#

DIR_HOME="/u/students/sawczualex/"
DIR_GRID="/vol/grid-solar/sgeusers/sawczualex/"
DIR_WORKSPACE="workspace/"
DIR_PROGRAM=$DIR_HOME$DIR_WORKSPACE/"HybridMOEAD/"
DIR_OUTPUT=$DIR_GRID$2 # Name of directory containing output

FILE_JOB_LIST="CURRENT_JOBS.txt"
FILE_RESULT_PREFIX="out"
FILE_FRONT_PREFIX="front"


#
# Copy the input files to the local directory
#

cp $DIR_PROGRAM"program.jar" .
cp $DIR_PROGRAM"moead.params" .
cp $1/* . # Copy datasets

echo ==WHATS THERE HAVING COPIED STUFF OVER AS INPUT==
ls -la

#
# Run the program
#

seed=$SGE_TASK_ID
result=$FILE_RESULT_PREFIX$seed.stat
front=$FILE_FRONT_PREFIX$seed.stat

java -cp program.jar moead.MOEAD $3 seed $seed outFileName $result frontFileName $front

echo ==AND NOW, HAVING DONE SOMETHING USEFUL AND CREATED SOME OUTPUT==
ls -la

# Now we move the output to a place to pick it up from later
if [ ! -d $DIR_OUTPUT ]; then
  mkdir $DIR_OUTPUT
fi
cp *.stat $DIR_OUTPUT

# Also copy over the parameter file, so we preserve the settings
cp moead.params $DIR_OUTPUT

echo "Ran through OK"


