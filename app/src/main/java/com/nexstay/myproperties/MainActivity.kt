package com.nexstay.myproperties

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nexstay.myproperties.ui.PropertyViewModel
import com.nexstay.myproperties.ui.screens.PropertyDetailScreen
import com.nexstay.myproperties.ui.screens.PropertyFormScreen
import com.nexstay.myproperties.ui.screens.PropertyListScreen
import com.nexstay.myproperties.ui.screens.PropertyMapScreen
import com.nexstay.myproperties.ui.theme.MyPropertiesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyPropertiesTheme {
                MyPropertiesNavHost()
            }
        }
    }
}

@Composable
fun MyPropertiesNavHost(viewModel: PropertyViewModel = viewModel()) {
    val navController = rememberNavController()
    val properties by viewModel.properties.collectAsState()

    NavHost(navController = navController, startDestination = "list") {

        composable("list") {
            PropertyListScreen(
                properties = properties,
                onAddClick = { navController.navigate("form") },
                onMapClick = { navController.navigate("map") },
                onPropertyClick = { navController.navigate("detail/${it.id}") }
            )
        }

        composable("map") {
            PropertyMapScreen(
                properties = properties,
                onBack = { navController.popBackStack() },
                onPropertyClick = { id -> navController.navigate("detail/$id") }
            )
        }

        composable(
            route = "detail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: return@composable
            val property by viewModel.property(id).collectAsState(initial = null)
            property?.let { current ->
                PropertyDetailScreen(
                    property = current,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate("form?id=$id") },
                    onDelete = {
                        viewModel.delete(current)
                        navController.popBackStack("list", inclusive = false)
                    }
                )
            }
        }

        composable(
            route = "form?id={id}",
            arguments = listOf(navArgument("id") {
                type = NavType.LongType
                defaultValue = 0L
            })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: 0L
            if (id == 0L) {
                PropertyFormScreen(
                    initial = null,
                    onBack = { navController.popBackStack() },
                    onSave = { draft ->
                        viewModel.save(draft)
                        navController.popBackStack()
                    }
                )
            } else {
                val existing by viewModel.property(id).collectAsState(initial = null)
                existing?.let { current ->
                    PropertyFormScreen(
                        initial = current,
                        onBack = { navController.popBackStack() },
                        onSave = { draft ->
                            viewModel.save(draft)
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}
