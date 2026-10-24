// Lists the devices in the token's team.
//     QUALITHM_API_TOKEN=qmt_... ./gradlew runExample -Pexample=listdevices
package listdevices

import com.qualithm.platform.PlatformClient
import com.qualithm.platform.PlatformError
import com.qualithm.platform.generated.apis.DevicesApi
import com.qualithm.platform.generated.apis.TeamsApi
import kotlin.system.exitProcess

fun main() {
    try {
        val platform = PlatformClient()
        // An API token belongs to one team, so its team list has exactly that team.
        val team = platform.call { api(::TeamsApi).listTeams() }.`data`.items.firstOrNull()
        if (team == null) {
            System.err.println("listdevices: the token has no team")
            exitProcess(1)
        }
        val page = platform.call { api(::DevicesApi).listDevices(team.id, limit = 20) }
        for (device in page.`data`.items) {
            println("${device.id}  ${device.spaceName}  ${device.name}")
        }
    } catch (error: PlatformError) {
        System.err.println("listdevices: ${error.message} (${error.code.value})")
        exitProcess(1)
    }
}
