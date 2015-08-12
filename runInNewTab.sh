pwd=$1
osascript -e "tell application \"Terminal\"" \
    -e "tell application \"System Events\" to keystroke \"t\" using {command down}" \
    -e "do script \"cd $pwd; sh ${*:2}\" in front window" \
    -e "end tell"
    > /dev/null

