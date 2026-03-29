#!/bin/bash

#set -x

git ls-files | grep -e "\(\.java\|\.h\|\.m\|\.mm\)$" | grep -v examples | while read -r file; do
    [ -f "$file" ] && clang-format -i "$file"
done

