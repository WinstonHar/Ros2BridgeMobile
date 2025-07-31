#!/bin/bash

PACKAGE_DIR="/app/cws/install/ryan_msgs/lib/python3.12/site-packages/ryan_msgs"

ACTION_NAMES=("RunPose")  # Add action names here

set -e  

if [ ! -d "$PACKAGE_DIR" ]; then
    echo "Error: Directory not found at '$PACKAGE_DIR'."
    exit 1
fi

PACKAGE_NAME=$(basename "$PACKAGE_DIR")

echo "Operating directly on installed package: $PACKAGE_NAME"
echo "Target Directory: $PACKAGE_DIR"
echo "-------------------------------------"


for ActionName in "${ACTION_NAMES[@]}"; do
    echo "Processing action: $ActionName"

    action_name_snake=$(echo "$ActionName" | sed 's/\(.\)\([A-Z]\)/\1_\2/g' | tr '[:upper:]' '[:lower:]')
    echo " -> Generated snake_case: $action_name_snake"

    action_init_file="$PACKAGE_DIR/action/__init__.py"
    line_to_add_action="from ._${action_name_snake} import ${ActionName}_Feedback, ${ActionName}_Result, ${ActionName}_Goal, ${ActionName}_SendGoal, ${ActionName}_GetResult"

    if [ ! -f "$action_init_file" ]; then
        echo "Error: $action_init_file does not exist."
        exit 1
    fi

    if ! grep -qF "$line_to_add_action" "$action_init_file"; then
        echo "$line_to_add_action" >> "$action_init_file"
        echo "   [MODIFIED] $action_init_file"
    else
        echo "   [SKIPPED] Line already exists in $action_init_file"
    fi

    srv_init_file="$PACKAGE_DIR/srv/__init__.py"
    line_to_add_srv="from ${PACKAGE_NAME}.action._${action_name_snake} import ${ActionName}_SendGoal, ${ActionName}_GetResult"

    if [ ! -f "$srv_init_file" ]; then
        echo "Error: $srv_init_file does not exist."
        exit 1
    fi

    if ! grep -qF "$line_to_add_srv" "$srv_init_file"; then
        echo "$line_to_add_srv" >> "$srv_init_file"
        echo "   [MODIFIED] $srv_init_file"
    else
        echo "   [SKIPPED] Line already exists in $srv_init_file"
    fi

    msg_init_file="$PACKAGE_DIR/msg/__init__.py"
    line_to_add_msg="from ${PACKAGE_NAME}.action import ${ActionName}_Feedback, ${ActionName}_Result, ${ActionName}_Goal"

    if [ ! -f "$msg_init_file" ]; then
        echo "Error: $msg_init_file does not exist."
        exit 1
    fi

    if ! grep -qF "$line_to_add_msg" "$msg_init_file"; then
        echo "$line_to_add_msg" >> "$msg_init_file"
        echo "   [MODIFIED] $msg_init_file"
    else
        echo "   [SKIPPED] Line already exists in $msg_init_file"
    fi

    echo "-------------------------------------"
done
