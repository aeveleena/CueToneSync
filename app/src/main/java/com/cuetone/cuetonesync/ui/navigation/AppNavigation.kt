package com.cuetone.cuetonesync.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cuetone.cuetonesync.data.DevicesRepository
import com.cuetone.cuetonesync.ble.CueToneBleScanner
import com.cuetone.cuetonesync.ble.CueToneBleRadioCoordinator
import com.cuetone.cuetonesync.ui.screens.ClapperDetailsScreen
import com.cuetone.cuetonesync.ui.screens.DeviceSettingsScreen
import com.cuetone.cuetonesync.ui.screens.DevicesScreen
import com.cuetone.cuetonesync.ui.screens.GeneratorDetailsScreen
import com.cuetone.cuetonesync.ui.screens.PacketLogScreen
import com.cuetone.cuetonesync.ui.screens.SlateRecordEditScreen
import com.cuetone.cuetonesync.ui.screens.SlateTableScreen
import com.cuetone.cuetonesync.ui.screens.projects.ProjectExplorerScreen
import com.cuetone.cuetonesync.ui.screens.projects.ProjectsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val bleScanner = remember { CueToneBleScanner(context.applicationContext) }

    DisposableEffect(bleScanner) {
        CueToneBleRadioCoordinator.registerScanner(bleScanner)
        onDispose {
            CueToneBleRadioCoordinator.unregisterScanner(bleScanner)
            bleScanner.shutdown()
        }
    }

    NavHost(
        navController = navController,
        startDestination = "devices"
    ) {
        composable("projects") {
            ProjectsScreen(
                onProjectClick = { projectId ->
                    navController.navigate("project/$projectId")
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("project/{projectId}") { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId")?.toLongOrNull()

            if (projectId != null) {
                ProjectExplorerScreen(
                    projectId = projectId,
                    folderId = null,
                    onBackClick = { navController.popBackStack() },
                    onFolderClick = { folderId ->
                        navController.navigate("project/$projectId/folder/$folderId")
                    },
                    onTableClick = { tableId ->
                        navController.navigate("slate_table/$tableId")
                    }
                )
            }
        }

        composable("project/{projectId}/folder/{folderId}") { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId")?.toLongOrNull()
            val folderId = backStackEntry.arguments?.getString("folderId")?.toLongOrNull()

            if (projectId != null && folderId != null) {
                ProjectExplorerScreen(
                    projectId = projectId,
                    folderId = folderId,
                    onBackClick = { navController.popBackStack() },
                    onFolderClick = { childFolderId ->
                        navController.navigate("project/$projectId/folder/$childFolderId")
                    },
                    onTableClick = { tableId ->
                        navController.navigate("slate_table/$tableId")
                    }
                )
            }
        }

        composable("devices") {
            DevicesScreen(
                bleScanner = bleScanner,
                onDeviceClick = { deviceId ->
                    val device = DevicesRepository.getDeviceById(deviceId)

                    if (device?.type == "Clapper") {
                        navController.navigate("clapper/$deviceId")
                    } else {
                        navController.navigate("generator/$deviceId")
                    }
                },
                onPacketLogClick = {
                    navController.navigate("packet_log")
                },
                onProjectsClick = {
                    navController.navigate("projects")
                }
            )
        }

        composable("generator/{deviceId}") { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId")?.toIntOrNull()

            if (deviceId != null) {
                GeneratorDetailsScreen(
                    deviceId = deviceId,
                    onBackClick = { navController.popBackStack() },
                    onSettingsClick = {
                        navController.navigate("device_settings/$deviceId")
                    }
                )
            }
        }

        composable("clapper/{deviceId}") { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId")?.toIntOrNull()

            if (deviceId != null) {
                ClapperDetailsScreen(
                    deviceId = deviceId,
                    onBackClick = { navController.popBackStack() },
                    onSettingsClick = {
                        navController.navigate("device_settings/$deviceId")
                    },
                    onSlateTableClick = {
                        navController.navigate("projects")
                    }
                )
            }
        }

        composable("device_settings/{deviceId}") { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId")?.toIntOrNull()

            if (deviceId != null) {
                DeviceSettingsScreen(
                    deviceId = deviceId,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        composable("slate_table/{tableId}") { backStackEntry ->
            val tableId = backStackEntry.arguments?.getString("tableId")?.toLongOrNull()

            if (tableId != null) {
                SlateTableScreen(
                    tableId = tableId,
                    onBackClick = { navController.popBackStack() },
                    onRecordClick = { recordId ->
                        navController.navigate("slate_record/$recordId")
                    }
                )
            }
        }

        composable("slate_record/{recordId}") { backStackEntry ->
            val recordId = backStackEntry.arguments?.getString("recordId")?.toLongOrNull()

            if (recordId != null) {
                SlateRecordEditScreen(
                    recordId = recordId,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        composable("packet_log") {
            PacketLogScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
