/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss

import java.io.FileDescriptor
import java.io.PrintWriter

interface BaseController {
    fun dumpState(prefix: String?, fd: FileDescriptor?, writer: PrintWriter?, dumpAll: Boolean)
}
