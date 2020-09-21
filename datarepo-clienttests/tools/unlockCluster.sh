#!/bin/bash

namespace=$1

if [ -z "$namespace" ]
then
  echo "namespace cannot be empty"
  exit 1
fi

kubectl delete secret -n $namespace "$namespace-inuse"
