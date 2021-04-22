#!/bin/bash

//usr/bin/env echo '
/**** BOOTSTRAP kscript ****\'>/dev/null
command -v kscript >/dev/null 2>&1 || curl -L "https://git.io/fpF1K" | bash 1>&2
exec kscript $0 "$@"
\*** IMPORTANT: Any code including imports and annotations must come after this line ***/


// Created with: kscript --add-bootstrap-header auto_install_ktscript.kts

println("tiempo")

/* EXAMPLE OF BASH SCRIPT ON KOTLIN:

import java.io.File

fun String.exec(dir: File? = null): Int {
    return ProcessBuilder("/bin/sh", "-c", this)
        .redirectErrorStream(true)
        .inheritIO()
        .directory(dir)
        .start()
        .waitFor()
}
 */
