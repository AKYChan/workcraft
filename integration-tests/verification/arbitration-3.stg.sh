script_file=${test_dir}/arbitration-3.stg.js

[[ -e $script_file ]] || error "Script file ${script_file} is missing"

./workcraft -nogui -dir:${test_dir} -exec:${script_file} >/dev/null
