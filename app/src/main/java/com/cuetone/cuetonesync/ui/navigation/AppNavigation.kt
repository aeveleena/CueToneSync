package com.cuetone.cuetonesync.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import com.cuetone.cuetonesync.data.DevicesRepository
import com.cuetone.cuetonesync.ui.screens.*

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "devices"
    ) {
        composable("devices") {
            DevicesScreen(
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
                        navController.navigate("slate_table")
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

        composable("slate_table") {
            SlateTableScreen(
                onBackClick = { navController.popBackStack() },
                onRecordClick = { recordId ->
                    navController.navigate("slate_record/$recordId")
                }
            )
        }

        composable("slate_record/{recordId}") { backStackEntry ->
            val recordId = backStackEntry.arguments?.getString("recordId")?.toIntOrNull()

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