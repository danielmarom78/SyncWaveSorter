#!/bin/bash

curl -X POST http://localhost:8080/api/v1/getparams.execute \
     -H 'Content-Type: application/json' \
     -d '{
          "applicationSetName": "fake-appset",
          "input": {
            "parameters": {
              "gitRepo": "https://github.com/danielmarom78/discount-appset.git",
              "gitPath": "dyn/apps",
              "cluster": "https://api.cluster-tjvw6.dynamic.redhatworkshops.io:6443",
              "namespace": "discount",
              "resourcePaths": [
                "resources.job.limits",
                "resources.deployment.limits"
              ]
            }
          }
        }' | jq .