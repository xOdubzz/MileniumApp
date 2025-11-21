package com.example.milenium

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.milenium.ui.theme.MileniumTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MileniumTheme {
                AppNavegacion()
            }
        }
    }
}

@Composable
fun AppNavegacion() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "inicio"
    ) {
        composable("inicio") { PantallaInicio(navController) }

        // PANTALLAS ORIGINALES EN BLANCO
        composable("perfil") { PantallaBlanca("Perfil") }
        composable("notificaciones") { PantallaBlanca("Notificaciones") }
        composable("busqueda") { PantallaBlanca("Buscar") }
        composable("nueva_post") { PantallaBlanca("Crear nueva publicación") }

        // NUEVAS PANTALLAS COPIADAS DE MILENIUM2
        composable("configuracion") { ConfiguracionScreen(navController) }
        composable("contactanos") { ContactanosScreen(navController) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaInicio(navController: NavController) {

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {

                Text(
                    text = "Menú",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Perfil") },
                    selected = false,
                    onClick = { navController.navigate("perfil") }
                )

                NavigationDrawerItem(
                    label = { Text("Inicio") },
                    selected = false,
                    onClick = { navController.navigate("inicio") }
                )

                NavigationDrawerItem(
                    label = { Text("Notificaciones") },
                    selected = false,
                    onClick = { navController.navigate("notificaciones") }
                )

                NavigationDrawerItem(
                    label = { Text("Contáctanos") },
                    selected = false,
                    onClick = { navController.navigate("contactanos") }
                )

                NavigationDrawerItem(
                    label = { Text("Configuración") },
                    selected = false,
                    onClick = { navController.navigate("configuracion") }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Menú"
                            )
                        }
                    },
                    title = {
                        Text(
                            "MILENIUM",
                            modifier = Modifier.clickable {
                                navController.navigate("inicio")
                            }
                        )
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate("busqueda") }) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Buscar"
                            )
                        }
                    }
                )
            },

            floatingActionButton = {
                FloatingActionButton(
                    onClick = { navController.navigate("nueva_post") }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Crear nueva publicación"
                    )
                }
            },

            floatingActionButtonPosition = FabPosition.Center
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    text = "Pantalla de inicio",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}

@Composable
fun PantallaBlanca(titulo: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.headlineLarge
        )
    }
}

// ==================== PANTALLA CONFIGURACIÓN (copiada tal cual de milenium2) ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionScreen(navController: NavController) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var isDarkMode by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("Español") }

    val currentTheme = if (isDarkMode) darkColorScheme() else lightColorScheme()

    MaterialTheme(colorScheme = currentTheme) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Configuración") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Filled.ArrowBack, "Volver")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Gray
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .background(Color.Transparent)
            ) {
                // MODO OSCURO
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Modo Oscuro", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.weight(1f))
                    Switch(checked = isDarkMode, onCheckedChange = { isDarkMode = it })
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // IDIOMA
                Text("Idioma", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    TextField(
                        readOnly = true,
                        value = selectedLanguage,
                        onValueChange = { },
                        label = { Text("Seleccionar idioma") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf("Español", "English").forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang) },
                                onClick = {
                                    selectedLanguage = lang
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // NOTIFICACIONES
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Notificaciones", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.weight(1f))
                    Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
                }

                Spacer(Modifier.height(32.dp))

                Button(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Volver a inicio")
                }
            }
        }
    }
}

// ==================== PANTALLA CONTÁCTANOS (copiada tal cual de milenium2) ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactanosScreen(navController: NavController) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Contáctanos", color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver",
                            tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Gray
                )
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .background(Color.White),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ContactItem(
                icon = Icons.Filled.Phone,
                text = "01 (238) 688 31 32",
                onClick = { }
            )
            ContactItem(
                icon = Icons.Filled.CheckCircle,
                text = "01 (238) 104 80 04",
                onClick = { }
            )
            ContactItem(
                icon = Icons.Filled.LocationOn,
                text = "Reforma Norte #444 Col. Centro\nC.P. 75700",
                onClick = { }
            )
            ContactItem(
                icon = Icons.Filled.Email,
                text = "admisiones@unimilenium.edu.mx",
                onClick = { }
            )
            ContactItem(
                icon = Icons.Filled.Face,
                text = "Centro Universitario Milenium",
                onClick = { }
            )
            ContactItem(
                icon = Icons.Filled.Clear,
                text = "unimilenium",
                onClick = { }
            )
        }
    }
}

@Composable
fun ContactItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.DarkGray
        )
    }
}