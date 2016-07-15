#!/bin/bash -e

err() { echo >&2 "$@"; exit 1; }

in="integration-tests/vme-tm.circuit.work"
got="integration-tests/vme-tm.v.got"
want="integration-tests/vme-tm.v"

./workcraft -nogui -exec:<(echo "export(load('$in'), '$got', 'VERILOG'); exit();")

[[ -f $got ]] || err "expected output file $got not found"

# size in bytes
size() { stat --printf="%s" $1; }

wantsize=$(size $want)
gotsize=$(size $got)

[[ $wantsize == $gotsize ]] || err "$want (expected) and $got have different sizes"

# keep it around if tests didn't succeed to help debugging
rm -f $got
