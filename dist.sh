#!/bin/sh -e

plugin_dirs="*Plugin/"
core_dir="WorkcraftCore"
core_files="LICENSE"
dist_dir="dist"

allplatforms="linux windows"
platforms="all"
bname="$(basename $0)"
tag="$(git describe --tags)"

usage() {
    cat <<EOF
$bname: create a distribution for Workcraft

Usage: $bname [platforms] [-h]

  platforms:  distribution platforms to build
                $allplatforms all (default: all)
  -h, --help: print this help
EOF
}

err() {
    echo "Error: $@" >&2
    exit 1
}

# Process parameters
for param in $*; do
    case $param in
        -h | --help)
            usage
            exit 0 ;;
        -t | --tag)
            tag="$2"
            shift 2 ;;
    esac
done

if [ ! -e "$core_dir/build" ]; then
    err "You need to run './gradlew assemble' first"
fi

if [ -z "$@" ] || [ "$@" == "all" ]; then
    platforms="$allplatforms"
else
    platforms="$@"
fi

for platform in $platforms; do

    dist_name="workcraft-${tag}-${platform}"
    dist_path="$dist_dir/$dist_name"
    template_dir="dist-template/$platform"

    echo $dist_name

    if [ ! -d "$template_dir" ]; then
        err "Template directory not found: $template_dir"
    fi

    if [ -e "$dist_path" ]; then
        err "Distribution directory already exists: $dist_path"
    fi

    mkdir -p $dist_path

    cp -r $template_dir/* $dist_path/

    cp $core_dir/build/libs/*.jar $dist_path/workcraft.jar

    mkdir -p $dist_path/plugins

    for d in $plugin_dirs; do
        cp $d/build/libs/*.jar $dist_path/plugins/
    done

    for d in doc/*; do
        cp -r $d $dist_path/
    done

    for f in $core_files; do
        cp $f $dist_path/
    done

    cd $dist_dir
    case $platform in
        windows)
            7z a -r ${dist_name}.zip $dist_name >/dev/null
            ;;
        linux)
            tar -czf ${dist_name}.tar.gz $dist_name
            ;;
    esac
    cd ..
done
