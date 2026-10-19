// Lists the workspace's devices.
//     QUALITHM_API_TOKEN=qmt_... ./gradlew runExample -Pexample=listdevices
package listdevices

import com.qualithm.platform.PlatformClient
import com.qualithm.platform.PlatformError
import com.qualithm.platform.generated.apis.DevicesApi
import kotlin.system.exitProcess

fun main() {
    try {
        val platform = PlatformClient()
        val page = platform.call { api(::DevicesApi).listDevices(limit = 20) }
        for (device in page.`data`.items) {
            println("${device.id}  ${device.spaceName}  ${device.name}")
        }
    } catch (error: PlatformError) {
        System.err.println("listdevices: ${error.message} (${error.code.value})")
        exitProcess(1)
    }
}
