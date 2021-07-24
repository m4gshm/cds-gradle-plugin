package com.github.m4gshm.cds.gradle

import java.io.Serializable

data class ClassListOptions(
    var checkSigned: Boolean = true,
    var analyzeConstantPool: Boolean = true,
    var superclass: Boolean = false,
    var interfaces: Boolean = false,
    var fields: Boolean = false,
    var onlyStaticFields: Boolean = false,
    var onlyFinalFields: Boolean = false,
    var methods: Boolean = true,
    var onlyStaticMethods: Boolean = true,
    var logSupportedClasses: Boolean = false,
    var logUnsupportedClasses: Boolean = false,
) : Serializable