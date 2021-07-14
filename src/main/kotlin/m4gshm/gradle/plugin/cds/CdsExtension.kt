package m4gshm.gradle.plugin.cds

import org.gradle.api.provider.Property

interface CdsExtension {

    val mainClass: Property<String>

}