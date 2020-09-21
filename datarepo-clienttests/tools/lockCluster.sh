#!/bin/bash

namespace=$1

if [ -z "$namespace" ]
then
  echo "namespace cannot be empty"
  exit 1
fi

if kubectl get secrets -n $namespace $namespace-inuse > /dev/null 2>&1; then
    printf "Namespace $namespace in use\n"
else
    printf "Namespace $namespace not in use, creating secret\n"
    kubectl create secret generic $namespace-inuse --from-literal=inuse=$namespace -n $namespace
fi
