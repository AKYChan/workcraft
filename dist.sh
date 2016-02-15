#!/bin/sh -e

plugin_dirs="*Plugin/"
core_dir="WorkcraftCore"
core_files="LICENSE README.md workcraft workcraft.bat"

dist_dir="dist"
template_dir="dist-template/linux"

description_msg="`basename $0`: creates a distribution for Workcraft"
usage_msg="Usage: `basename $0` [-d DIST_DIR] [-t TEMPLATE_DIR] [-h | --help]"
params_msg="
  -d DIST_DIR : distribution directory (default: $dist_dir)
  -t TEMPLATE_DIR: template directory (default: $template_dir))
  -h, --help : print this help"
help_msg="${description_msg}\n\n${usage_msg}\n${params_msg}\n"

err() {
    echo "Error: $@" >&2
    exit 1
}

# Process parameters
for param in $*; do
    case $param in
        -d) dist_dir=$2; shift 2;;
        -t) template_dir=$2; shift 2;;
        -h | --help) printf "$help_msg"; exit 0;
    esac
done

if [ ! -e "$core_dir/build" ]; then
    err "You need to run 'gradle assemble' first"
fi

if [ ! -d "$template_dir" ]; then
    err "Template directory not found: $template_dir"
fi

if [ -e "$dist_dir" ]; then
    err "Distribution directory already exists: $dist_dir"
fi

mkdir -p $dist_dir

cp -r $template_dir/* $dist_dir/

cp $core_dir/build/libs/*.jar $dist_dir/workcraft.jar

mkdir -p $dist_dir/plugins

for d in $plugin_dirs; do
    cp $d/build/libs/*.jar $dist_dir/plugins/
done

for d in doc/*; do
    cp -r $d $dist_dir/
done

for f in $core_files; do
    cp $f $dist_dir/
done
