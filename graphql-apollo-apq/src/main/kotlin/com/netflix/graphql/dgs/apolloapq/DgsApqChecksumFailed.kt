package com.netflix.graphql.dgs.apolloapq

import graphql.ErrorClassification

class DgsApqChecksumFailed : RuntimeException("Provided persisted query sha does not match query"),
    ErrorClassification {
}
