import xyz.wagyourtail.jvmdg.gradle.task.files.DowngradeFiles

plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

tasks.withType<JavaCompile>().configureEach {
    options.annotationProcessorPath = configurations.annotationProcessor.get()
}

// JVMDowngrader 2.0.1 omits output directories when their compiled inputs do not exist yet.
// Declare class outputs lazily so a clean build discovers and runs tests on its first invocation.
tasks.withType<DowngradeFiles>().configureEach {
    if (name == "downgradeMainClasses" || name == "downgradeTestClasses") {
        outputs.dirs(provider { outputMap.values })
    }
}
