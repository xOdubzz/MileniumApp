package com.example.milenium

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        composable("perfil") { 
            SimpleProfileScreen(onBack = { navController.popBackStack() }) 
        }
        composable("notificaciones") { 
            NotificationScreenStyle(onBack = { navController.popBackStack() }) 
        }
        composable("contactanos") { PantallaBlanca("Contáctanos") }
        composable("configuracion") { PantallaBlanca("Configuración") }
        composable("busqueda") { PantallaBlanca("Buscar") }
        composable("nueva_post") { PantallaBlanca("Crear nueva publicación") }
    }
}

data class Publication(
    val id: Int,
    val author: String,
    val handle: String,
    val time: String,
    val content: String,
    val isLiked: Boolean = false
)

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
                    onClick = { 
                        scope.launch { drawerState.close() }
                        navController.navigate("perfil") 
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Inicio") },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        navController.navigate("inicio") 
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Notificaciones") },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        navController.navigate("notificaciones") 
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Contáctanos") },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        navController.navigate("contactanos") 
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Configuración") },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        navController.navigate("configuracion") 
                    }
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
                            "LOGO",
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
fun PublicationCardModern(
    publication: Publication,
    onDelete: () -> Unit,
    onLike: (Boolean) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEEEEEE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = publication.author,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                    Text(
                        text = "${publication.handle} • ${publication.time}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // Menú de opciones
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = Color.Gray)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Eliminar") },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.Delete, contentDescription = null)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = publication.content,
                fontSize = 14.sp,
                color = Color.Black,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Divider(color = Color(0xFFF0F0F0), thickness = 1.dp)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { onLike(!publication.isLiked) }) {
                    Icon(
                        imageVector = if (publication.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (publication.isLiked) Color(0xFF5F4776) else Color.Black
                    )
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Filled.Share, contentDescription = "Share", tint = Color.Gray)
                }
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

// ------------------------- NUEVAS PANTALLAS -------------------------

@Composable
fun SimpleProfileScreen(onBack: () -> Unit) {
    // Lista de publicaciones específicas del perfil
    val publications = remember {
        mutableStateListOf(
            Publication(1, "Rafael", "@rafael", "Hace 2 horas", "Buen trabajo Pedro! Sigue trabajando en eso!"),
            Publication(2, "Milenium Oficial", "@milenium", "Hace 3 horas", "Repost by @Centro\n\ntexto de ejemplo xd")
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White), // Fondo blanco explícito
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = Color.Black, // Icono negro
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Rafael",
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp,
                                color = Color.Black // Texto negro
                            )
                            Text(
                                text = "Pantera Osorio",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.DarkGray
                            )
                            Text(
                                text = "Ing. Sistemas Informáticos e\nInteligencia Artificial",
                                fontSize = 14.sp,
                                color = Color.DarkGray,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Redes",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.Black // Texto negro
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(start = 16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color.LightGray, CircleShape)
                                    .background(Color(0xFFEEEEEE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccountCircle,
                                    contentDescription = "Foto de perfil",
                                    modifier = Modifier.size(80.dp),
                                    tint = Color.Gray
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "0",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.Black // Texto negro
                            )
                            Text(
                                text = "Posts",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "BIOGRAFIA",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = Color.LightGray, thickness = 1.dp)
            }
        }

        items(publications) { publication ->
            PublicationCardModern(
                publication = publication,
                onDelete = {
                    publications.remove(publication)
                },
                onLike = { liked ->
                    val index = publications.indexOf(publication)
                    if (index != -1) {
                        publications[index] = publication.copy(isLiked = liked)
                    }
                }
            )
        }
    }
}

@Composable
fun FeedPostItem(index: Int) {
    var isLiked by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEEEEEE)),
                contentAlignment = Alignment.Center
            ) {
                 Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = if (index == 0) "Rafael" else "Milenium Oficial",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black // Texto negro
                )
                Text(
                    text = if (index == 0) "Hace 2 horas" else "Repost by @Centro",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (index == 0) "Buen trabajo Pedro! Sigue trabajando en eso!" else "texto de ejemplo xd",
            fontSize = 14.sp,
            color = Color.Black // Texto negro
        )

        Spacer(modifier = Modifier.height(10.dp))

        Divider(color = Color.LightGray, thickness = 1.dp)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, 
                contentDescription = "Like", 
                tint = if (isLiked) Color(0xFF5F4776) else Color.Black,
                modifier = Modifier.clickable { isLiked = !isLiked }
            )
            Icon(Icons.Filled.Share, contentDescription = "Share", tint = Color.Gray)
            Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = Color.Gray)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = Color(0xFFEEEEEE), thickness = 5.dp) 
    }
}

@Composable
fun NotificationScreenStyle(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // Fondo blanco explícito
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.Black) // Icono negro
            }
            
            Text(
                text = "Notificaciones",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black // Texto negro
            )
            Icon(Icons.Filled.Search, contentDescription = "Buscar", tint = Color.Black) // Icono negro
        }
        
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(3) { index ->
                NotificationItemStyle(index)
            }
        }
    }
}

@Composable
fun NotificationItemStyle(index: Int) {
    val names = listOf("Pedro", "Pedro", "Willmar")
    val actions = listOf("Le dio me gusta", "Le dio me gusta", "@ Te mencionó")
    val times = listOf("5 mins", "5 mins", "1 hr")
    val isLike = index < 2 

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.AccountCircle,
            contentDescription = null,
            modifier = Modifier.size(50.dp),
            tint = Color.LightGray
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = names.getOrElse(index) { "Usuario" },
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.Black // Texto negro explícito
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLike) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = Color(0xFF5F4776), 
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = actions.getOrElse(index) { "" },
                    color = Color.DarkGray, // Texto gris oscuro
                    fontSize = 14.sp
                )
            }
        }
        
        Text(
            text = times.getOrElse(index) { "" },
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
    Divider(color = Color(0xFFF5F5F5), thickness = 1.dp)
}
