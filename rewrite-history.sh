#!/bin/sh
#now rewrite the history stuff
export KEEP=`printf "%s " *`
git filter-branch -f --prune-empty --msg-filter 'cat && echo "" && echo "was: $GIT_COMMIT"' --index-filter 'git rm --cached -r -q -- . ; git reset -q $GIT_COMMIT -- $KEEP' -- --all

