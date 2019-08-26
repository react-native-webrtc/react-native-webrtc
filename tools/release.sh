#!/bin/bash

version="$1"

# --- check args
if [ -z "$version" ]; then
	echo -e 'version arg is required.'
	echo -e 'Usage: bash tools/release.sh $version'
	exit 1
fi

# --- check package.json is in the same dir.
if [ ! -f ./package.json ]; then
	echo -e "package.json not found in the current directory."
	echo -e 'Usage: bash tools/release.sh $version'
	exit 1
fi

# --- check tag is exist
if git rev-parse "$version" >/dev/null 2>&1; then
	echo -e "version tag $version already exist."
	exit 1
fi

# --- try to bump version of package.json, fail if duplicated.
npm version "$version" --no-git-tag-version
git_changes=$(git status --porcelain | grep package.json)
if [ -z "${git_changes}" ]; then
	echo "npm bump failed"
	exit 1
fi

last_version=`git describe --tags --abbrev=0`
git_log_cmd='git log '$last_version'..HEAD --no-merges --pretty=format:%h%x20%s%x20%x20%x28%x20%an%x20%ad%x29 --date=iso'

# --- commit with change logs
git add package.json package-lock.json
git commit -m "release ${version}" -m "$($git_log_cmd)"
git tag -a "$version" -m "$($git_log_cmd)"

# --- success messages
echo
echo -e "===== Version $version released! ====="
echo
echo -e "USE: \"git push origin master --tags\" to push to master"
echo -e "USE: \"git reset --hard origin/master && git tag -d $version\" to rollback release"
echo
