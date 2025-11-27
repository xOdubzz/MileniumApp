package com.example.milenium

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// =======================================
// ========= PALETA DE COLORES MORADA ====
// =======================================

private object PurpleTheme {
    val PrimaryPurple = Color(0xFF8B5FBF)
    val PrimaryLight = Color(0xFFB39DDB)
    val PrimaryDark = Color(0xFF6A0DAD)
    val SecondaryPurple = Color(0xFF9C27B0)
    val AccentPurple = Color(0xFFE1BEE7)
    val SurfacePurple = Color(0xFFF3E5F5)
    val BackgroundPurple = Color(0xFFEDE7F6)
    val TextPurple = Color(0xFF4A148C)

    // Gradientes morados
    val PurpleGradient = Brush.verticalGradient(
        colors = listOf(PrimaryPurple, PrimaryDark)
    )
    val LightPurpleGradient = Brush.verticalGradient(
        colors = listOf(SurfacePurple, BackgroundPurple)
    )
    val CardGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFF3E5F5), Color(0xFFEDE7F6))
    )
    val ButtonGradient = Brush.horizontalGradient(
        colors = listOf(PrimaryPurple, SecondaryPurple)
    )
}

// Tema morado personalizado
private val PurpleColorScheme = lightColorScheme(
    primary = PurpleTheme.PrimaryPurple,
    onPrimary = Color.White,
    primaryContainer = PurpleTheme.SurfacePurple,
    onPrimaryContainer = PurpleTheme.PrimaryDark,
    secondary = PurpleTheme.SecondaryPurple,
    onSecondary = Color.White,
    secondaryContainer = PurpleTheme.AccentPurple,
    onSecondaryContainer = PurpleTheme.PrimaryDark,
    background = PurpleTheme.BackgroundPurple,
    onBackground = PurpleTheme.TextPurple,
    surface = PurpleTheme.SurfacePurple,
    onSurface = PurpleTheme.TextPurple,
    surfaceVariant = Color(0xFFE1BEE7),
    onSurfaceVariant = PurpleTheme.PrimaryDark
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = PurpleColorScheme
            ) {
                AppNavegacion()
            }
        }
    }
}

// =======================================
// ============ CONSTANTS ================
// =======================================

private object Constants {
    const val DATASTORE_NAME = "publications"
    const val PUBLICATIONS_KEY = "publications_list"
    const val USERS_KEY = "users_data"
    const val IMAGE_HEIGHT = 200
    const val DEFAULT_PADDING = 16
    const val SMALL_SPACING = 8
    const val MEDIUM_SPACING = 16
    const val LARGE_SPACING = 23
}

// =======================================
// ============ DATA LAYER ===============
// =======================================

@Serializable
data class Publication(
    val titulo: String,
    val descripcion: String,
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val likes: Int = 0,
    val isLiked: Boolean = false
)

@Serializable
data class User(
    val id: Int,
    val nombre: String,
    val edad: Int,
    val profesion: String,
    val descripcion: String,
    val imagenUrl: String? = null,
    val intereses: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class SearchResult(
    val publications: List<Publication>,
    val users: List<User>
)

private val Context.dataStore by preferencesDataStore(Constants.DATASTORE_NAME)

class PublicationRepository(private val context: Context) {
    private val publicationsKey = stringPreferencesKey(Constants.PUBLICATIONS_KEY)

    suspend fun savePublication(publication: Publication) {
        val currentList = getAllPublicationsList()
        val updatedList = currentList + publication
        context.dataStore.edit { preferences ->
            preferences[publicationsKey] = Json.encodeToString(updatedList)
        }
    }

    suspend fun updatePublicationLike(publicationId: Long, isLiked: Boolean) {
        val currentList = getAllPublicationsList()
        val updatedList = currentList.map { publication ->
            if (publication.timestamp == publicationId) {
                publication.copy(
                    likes = if (isLiked) publication.likes + 1 else maxOf(0, publication.likes - 1),
                    isLiked = isLiked
                )
            } else {
                publication
            }
        }
        context.dataStore.edit { preferences ->
            preferences[publicationsKey] = Json.encodeToString(updatedList)
        }
    }

    suspend fun deletePublication(publicationToDelete: Publication) {
        val currentList = getAllPublicationsList()
        val updatedList = currentList.filterNot {
            it.timestamp == publicationToDelete.timestamp &&
                    it.titulo == publicationToDelete.titulo
        }
        context.dataStore.edit { preferences ->
            preferences[publicationsKey] = Json.encodeToString(updatedList)
        }

        publicationToDelete.imageUri?.let { uriString ->
            deleteImageFile(uriString)
        }
    }

    fun getAllPublications(): Flow<List<Publication>> {
        return context.dataStore.data.map { preferences ->
            val jsonString = preferences[publicationsKey] ?: "[]"
            val publications = Json.decodeFromString<List<Publication>>(jsonString)
            publications.sortedByDescending { it.timestamp }
        }
    }

    private suspend fun getAllPublicationsList(): List<Publication> {
        val preferences = context.dataStore.data.first()
        val jsonString = preferences[publicationsKey] ?: "[]"
        return Json.decodeFromString(jsonString)
    }

    fun searchPublications(query: String, publications: List<Publication>): List<Publication> {
        val filtered = if (query.isBlank()) publications
        else publications.filter {
            it.titulo.contains(query, ignoreCase = true) ||
                    it.descripcion.contains(query, ignoreCase = true)
        }
        return filtered.sortedByDescending { it.timestamp }
    }

    private fun deleteImageFile(uriString: String) {
        try {
            if (uriString.contains("fileprovider")) {
                val uri = Uri.parse(uriString)
                val pathSegments = uri.pathSegments
                if (pathSegments.isNotEmpty()) {
                    val fileName = pathSegments.last()
                    val storageDir = File(context.filesDir, "publication_images")
                    val imageFile = File(storageDir, fileName)
                    if (imageFile.exists()) {
                        imageFile.delete()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun searchAllContent(query: String, publications: List<Publication>): SearchResult {
        val userRepository = UserRepository(context)

        val filteredPublications = searchPublications(query, publications)
        val filteredUsers = userRepository.searchUsers(query)

        return SearchResult(
            publications = filteredPublications,
            users = filteredUsers
        )
    }
}

class UserRepository(private val context: Context) {
    private val dataStore = context.dataStore
    private val usersKey = stringPreferencesKey(Constants.USERS_KEY)

    private val defaultUsers = listOf(
        User(
            id = 1,
            nombre = "María González",
            edad = 25,
            profesion = "Diseñadora UX",
            descripcion = "Apasionada por el diseño y la tecnología. Me encanta crear experiencias increíbles.",
            intereses = listOf("Diseño", "Tecnología", "Arte", "Fotografía")
        ),
        User(
            id = 2,
            nombre = "Carlos Rodríguez",
            edad = 32,
            profesion = "Desarrollador Android",
            descripcion = "Desarrollador con 5 años de experiencia en apps móviles.",
            intereses = listOf("Programación", "Deportes", "Videojuegos")
        ),
        User(
            id = 3,
            nombre = "Ana López",
            edad = 28,
            profesion = "Ingeniera de Software",
            descripcion = "Especializada en desarrollo backend y arquitectura de sistemas.",
            intereses = listOf("Programación", "Música", "Viajes")
        ),
        User(
            id = 4,
            nombre = "David López",
            edad = 45,
            profesion = "Arquitecto",
            descripcion = "Diseño espacios innovadores que mejoran la calidad de vida.",
            intereses = listOf("Arquitectura", "Diseño", "Sostenibilidad")
        ),
        User(
            id = 5,
            nombre = "Laura Sánchez",
            edad = 22,
            profesion = "Estudiante de Medicina",
            descripcion = "Futura médica especializada en pediatría.",
            intereses = listOf("Medicina", "Salud", "Deportes", "Música")
        ),
        User(
            id = 6,
            nombre = "Javier Pérez",
            edad = 38,
            profesion = "Chef Ejecutivo",
            descripcion = "Creando experiencias culinarias únicas con ingredientes locales.",
            intereses = listOf("Cocina", "Viajes", "Fotografía")
        ),
        User(
            id = 7,
            nombre = "Elena Castro",
            edad = 29,
            profesion = "Marketing Digital",
            descripcion = "Especialista en estrategias de marketing para redes sociales.",
            intereses = listOf("Marketing", "Redes Sociales", "Fotografía")
        ),
        User(
            id = 8,
            nombre = "Miguel Torres",
            edad = 51,
            profesion = "Profesor Universitario",
            descripcion = "Enseñando la próxima generación de ingenieros.",
            intereses = listOf("Educación", "Tecnología", "Investigación")
        ),
        User(
            id = 9,
            nombre = "Sofía Ramírez",
            edad = 26,
            profesion = "Psicóloga Clínica",
            descripcion = "Ayudando a las personas a encontrar su bienestar mental.",
            intereses = listOf("Psicología", "Salud Mental", "Yoga")
        ),
        User(
            id = 10,
            nombre = "Roberto Díaz",
            edad = 34,
            profesion = "Emprendedor",
            descripcion = "Fundador de varias startups tecnológicas exitosas.",
            intereses = listOf("Emprendimiento", "Tecnología", "Inversiones")
        )
    )

    suspend fun getAllUsers(): List<User> {
        val preferences = dataStore.data.first()
        val jsonString = preferences[usersKey]

        return if (jsonString.isNullOrBlank()) {
            saveUsers(defaultUsers)
            defaultUsers
        } else {
            Json.decodeFromString<List<User>>(jsonString)
        }
    }

    private suspend fun saveUsers(users: List<User>) {
        dataStore.edit { preferences ->
            preferences[usersKey] = Json.encodeToString(users)
        }
    }

    suspend fun searchUsers(query: String): List<User> {
        if (query.isBlank()) return emptyList()

        val allUsers = getAllUsers()
        return allUsers.filter { user ->
            user.nombre.contains(query, ignoreCase = true) ||
                    user.profesion.contains(query, ignoreCase = true) ||
                    user.descripcion.contains(query, ignoreCase = true) ||
                    user.intereses.any { it.contains(query, ignoreCase = true) }
        }
    }

    suspend fun getUserById(id: Int): User? {
        val allUsers = getAllUsers()
        return allUsers.find { it.id == id }
    }

    suspend fun getUsersByAgeRange(minAge: Int, maxAge: Int): List<User> {
        val allUsers = getAllUsers()
        return allUsers.filter { it.edad in minAge..maxAge }
    }

    suspend fun addUser(user: User) {
        val currentUsers = getAllUsers().toMutableList()
        currentUsers.add(user)
        saveUsers(currentUsers)
    }
}

// =======================================
// ============ IMAGE MANAGER ============
// =======================================

class ImageManager(private val context: Context) {

    fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = File(context.filesDir, "publication_images")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        return File(storageDir, "publication_${timeStamp}.jpg")
    }

    fun getPersistentUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun isUriAccessible(uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri) != null
        } catch (e: Exception) {
            false
        }
    }

    fun getDisplayableUri(uriString: String?): Uri? {
        if (uriString.isNullOrBlank()) return null
        return try {
            Uri.parse(uriString)
        } catch (e: Exception) {
            null
        }
    }

    fun copyUriToPersistentFile(temporaryUri: Uri): Uri? {
        return try {
            val persistentFile = createImageFile()
            context.contentResolver.openInputStream(temporaryUri)?.use { inputStream ->
                persistentFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            getPersistentUri(persistentFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

// =======================================
// ============ NAVIGATION ===============
// =======================================

@Composable
fun AppNavegacion() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "publicaciones"
    ) {
        composable("publicaciones") {
            HomeScreen(navController = navController)
        }
        composable("crear_publicacion") {
            CreateScreen(
                navController = navController,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("buscar") {
            SearchScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("perfil") {
            SimpleProfileScreen(onBack = { navController.popBackStack() })
        }
        composable("notificaciones") {
            NotificationScreenStyle(onBack = { navController.popBackStack() })
        }
        composable("contactanos") {
            ContactanosScreen(navController = navController)
        }
        composable("configuracion") {
            ConfiguracionScreen(navController = navController)
        }
        composable("perfil_usuario/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")?.toIntOrNull() ?: 0
            UserProfileScreen(userId = userId)
        }
    }
}

// =======================================
// ========= PANTALLA PRINCIPAL ==========
// =======================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val repository = remember { PublicationRepository(context) }
    val publications by repository.getAllPublications().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scopeDrawer = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.background(PurpleTheme.BackgroundPurple)
            ) {
                Text(
                    text = "Menú",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp),
                    color = PurpleTheme.PrimaryDark
                )

                NavigationDrawerItem(
                    label = {
                        Text("Publicaciones", color = PurpleTheme.PrimaryDark)
                    },
                    selected = true,
                    onClick = {
                        navController.navigate("publicaciones")
                        scopeDrawer.launch { drawerState.close() }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = PurpleTheme.SurfacePurple,
                        unselectedContainerColor = Color.Transparent
                    )
                )

                NavigationDrawerItem(
                    label = {
                        Text("Perfil", color = PurpleTheme.PrimaryDark)
                    },
                    selected = false,
                    onClick = {
                        navController.navigate("perfil")
                        scopeDrawer.launch { drawerState.close() }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = PurpleTheme.SurfacePurple,
                        unselectedContainerColor = Color.Transparent
                    )
                )

                NavigationDrawerItem(
                    label = {
                        Text("Notificaciones", color = PurpleTheme.PrimaryDark)
                    },
                    selected = false,
                    onClick = {
                        navController.navigate("notificaciones")
                        scopeDrawer.launch { drawerState.close() }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = PurpleTheme.SurfacePurple,
                        unselectedContainerColor = Color.Transparent
                    )
                )

                NavigationDrawerItem(
                    label = {
                        Text("Contáctanos", color = PurpleTheme.PrimaryDark)
                    },
                    selected = false,
                    onClick = {
                        navController.navigate("contactanos")
                        scopeDrawer.launch { drawerState.close() }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = PurpleTheme.SurfacePurple,
                        unselectedContainerColor = Color.Transparent
                    )
                )

                NavigationDrawerItem(
                    label = {
                        Text("Configuración", color = PurpleTheme.PrimaryDark)
                    },
                    selected = false,
                    onClick = {
                        navController.navigate("configuracion")
                        scopeDrawer.launch { drawerState.close() }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = PurpleTheme.SurfacePurple,
                        unselectedContainerColor = Color.Transparent
                    )
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = {
                            scopeDrawer.launch { drawerState.open() }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Menú",
                                tint = Color.White
                            )
                        }
                    },
                    title = {
                        Image(
                            painter = painterResource(id = R.drawable.logo2),
                            contentDescription = "Logo Milenium",
                            modifier = Modifier
                                .size(60.dp)
                                .clickable { navController.navigate("publicaciones") }
                        )
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate("buscar") }) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Buscar",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = PurpleTheme.PrimaryPurple
                    )
                )
            },

            floatingActionButton = {
                FloatingActionButton(
                    onClick = { navController.navigate("crear_publicacion") },
                    containerColor = PurpleTheme.PrimaryPurple,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Crear nueva publicación"
                    )
                }
            },

            floatingActionButtonPosition = FabPosition.Center
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PurpleTheme.BackgroundPurple)
            ) {
                if (publications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "No hay publicaciones",
                                modifier = Modifier.size(64.dp),
                                tint = PurpleTheme.PrimaryPurple
                            )
                            Spacer(modifier = Modifier.height(Constants.MEDIUM_SPACING.dp))
                            Text(
                                text = "No hay publicaciones",
                                style = MaterialTheme.typography.titleMedium,
                                color = PurpleTheme.PrimaryDark
                            )
                            Spacer(modifier = Modifier.height(Constants.SMALL_SPACING.dp))
                            Text(
                                text = "Haz clic en el botón + para crear tu primera publicación",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = PurpleTheme.PrimaryDark
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(publications) { publication ->
                            PublicationCardModern(
                                publication = publication,
                                onDelete = {
                                    scope.launch {
                                        repository.deletePublication(publication)
                                    }
                                },
                                onLike = { liked ->
                                    scope.launch {
                                        repository.updatePublicationLike(publication.timestamp, liked)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// =======================================
// ======== COMPONENTE PUBLICACIÓN =======
// =======================================

@Composable
fun PublicationCardModern(
    publication: Publication,
    onDelete: () -> Unit,
    onLike: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isLiked by remember { mutableStateOf(publication.isLiked) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val imageManager = remember { ImageManager(context) }

    val displayUri = remember(publication.imageUri) {
        publication.imageUri?.let { imageManager.getDisplayableUri(it) }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text("Eliminar Publicación", color = PurpleTheme.PrimaryDark)
            },
            text = {
                Text("¿Estás seguro de que quieres eliminar esta publicación? Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("Eliminar", color = PurpleTheme.PrimaryPurple)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancelar", color = PurpleTheme.PrimaryDark)
                }
            },
            containerColor = PurpleTheme.SurfacePurple
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                clip = false
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = PurpleTheme.SurfacePurple
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PurpleTheme.CardGradient)
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar con gradiente morado
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .background(PurpleTheme.PurpleGradient)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "U",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Usuario",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PurpleTheme.PrimaryDark
                    )
                    Text(
                        text = formatDate(publication.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = PurpleTheme.PrimaryDark.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opciones",
                        tint = PurpleTheme.PrimaryDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Contenido
            Text(
                text = publication.titulo,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = PurpleTheme.PrimaryDark
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = publication.descripcion,
                style = MaterialTheme.typography.bodyMedium,
                color = PurpleTheme.PrimaryDark.copy(alpha = 0.8f)
            )

            // Imagen
            displayUri?.let { uri ->
                Spacer(modifier = Modifier.height(16.dp))
                AsyncImage(
                    model = uri,
                    contentDescription = "Imagen de la publicación: ${publication.titulo}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Constants.IMAGE_HEIGHT.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            2.dp,
                            PurpleTheme.PrimaryLight.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentScale = ContentScale.Crop
                )
            } ?: run {
                if (!publication.imageUri.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(Constants.MEDIUM_SPACING.dp))
                    Text(
                        text = "⚠️ Imagen no disponible",
                        style = MaterialTheme.typography.bodySmall,
                        color = PurpleTheme.PrimaryPurple
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer con acciones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Botón de Like
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        isLiked = !isLiked
                        onLike(isLiked)
                    }
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.Favorite,
                        contentDescription = "Like",
                        tint = if (isLiked) PurpleTheme.PrimaryPurple else PurpleTheme.PrimaryDark.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${publication.likes + if (isLiked && !publication.isLiked) 1 else if (!isLiked && publication.isLiked) -1 else 0}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isLiked) PurpleTheme.PrimaryPurple else PurpleTheme.PrimaryDark.copy(alpha = 0.6f)
                    )
                }

                // Botón de Compartir
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Compartir",
                        tint = PurpleTheme.PrimaryDark.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Compartir",
                        style = MaterialTheme.typography.bodySmall,
                        color = PurpleTheme.PrimaryDark
                    )
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

// =======================================
// ============ COMPONENTS ===============
// =======================================

@Composable
fun UserCard(
    user: User,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = PurpleTheme.SurfacePurple
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PurpleTheme.CardGradient)
                .padding(Constants.DEFAULT_PADDING.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar del usuario con gradiente morado
            Surface(
                modifier = Modifier.size(60.dp),
                shape = CircleShape,
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier.background(PurpleTheme.PurpleGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.nombre.take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(Constants.MEDIUM_SPACING.dp))

            // Información del usuario
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PurpleTheme.PrimaryDark
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${user.edad} años • ${user.profesion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PurpleTheme.PrimaryDark.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = user.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    color = PurpleTheme.TextPurple
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Intereses
                if (user.intereses.isNotEmpty()) {
                    Text(
                        text = "Intereses: ${user.intereses.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PurpleTheme.PrimaryPurple
                    )
                }
            }
        }
    }
}

@Composable
fun UserProfileScreen(userId: Int) {
    val context = LocalContext.current
    val userRepository = remember { UserRepository(context) }
    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        user = userRepository.getUserById(userId)
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PurpleTheme.BackgroundPurple)
    ) {
        SimpleTopAppBar(
            title = "Perfil de Usuario",
            onBackClick = { /* Manejar navegación back */ }
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PurpleTheme.PrimaryPurple)
            }
        } else if (user == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Usuario no encontrado", color = PurpleTheme.PrimaryDark)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    UserCard(user = user!!)
                }
            }
        }
    }
}

@Composable
fun SimpleTopAppBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(PurpleTheme.PrimaryPurple)
            .padding(horizontal = Constants.DEFAULT_PADDING.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (onBackClick != null) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White
                )
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            color = Color.White
        )

        Row {
            actions()
        }
    }
}

// =======================================
// ============== SCREENS ================
// =======================================

@Composable
fun CreateScreen(
    navController: NavHostController,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { PublicationRepository(context) }
    val imageManager = remember { ImageManager(context) }
    val scope = rememberCoroutineScope()

    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf("") }

    val isFormValid = titulo.isNotBlank() && descripcion.isNotBlank()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                val persistentUri = imageManager.copyUriToPersistentFile(uri)
                imageUri = persistentUri
                if (persistentUri == null) {
                    message = "Error al guardar la imagen"
                }
            }
        }
    )

    var currentPhotoFile by remember { mutableStateOf<File?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success: Boolean ->
            if (success && currentPhotoFile != null) {
                val persistentUri = imageManager.getPersistentUri(currentPhotoFile!!)
                imageUri = persistentUri
            } else {
                message = "Error al tomar la foto"
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PurpleTheme.BackgroundPurple)
    ) {
        SimpleTopAppBar(
            title = "Crear Publicación",
            onBackClick = onNavigateBack
        )

        Divider(color = PurpleTheme.PrimaryLight)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Constants.DEFAULT_PADDING.dp)
        ) {
            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it },
                label = { Text("Título", color = PurpleTheme.PrimaryDark) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = PurpleTheme.SurfacePurple,
                    unfocusedContainerColor = PurpleTheme.SurfacePurple,
                    focusedBorderColor = PurpleTheme.PrimaryPurple,
                    unfocusedBorderColor = PurpleTheme.PrimaryLight,
                )
            )

            Spacer(modifier = Modifier.height(Constants.MEDIUM_SPACING.dp))

            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción", color = PurpleTheme.PrimaryDark) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = PurpleTheme.SurfacePurple,
                    unfocusedContainerColor = PurpleTheme.SurfacePurple,
                    focusedBorderColor = PurpleTheme.PrimaryPurple,
                    unfocusedBorderColor = PurpleTheme.PrimaryLight,
                )
            )

            Spacer(modifier = Modifier.height(Constants.MEDIUM_SPACING.dp))

            Text(
                text = "Seleccionar imagen:",
                style = MaterialTheme.typography.labelLarge,
                color = PurpleTheme.PrimaryDark
            )

            Spacer(modifier = Modifier.height(Constants.SMALL_SPACING.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        galleryLauncher.launch("image/*")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleTheme.PrimaryPurple
                    )
                ) {
                    Text("Galería", color = Color.White)
                }

                Button(
                    onClick = {
                        val photoFile = imageManager.createImageFile()
                        currentPhotoFile = photoFile
                        val photoUri = imageManager.getPersistentUri(photoFile)
                        cameraLauncher.launch(photoUri)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleTheme.SecondaryPurple
                    )
                ) {
                    Text("Cámara", color = Color.White)
                }
            }

            imageUri?.let { uri ->
                Spacer(modifier = Modifier.height(Constants.MEDIUM_SPACING.dp))
                AsyncImage(
                    model = uri,
                    contentDescription = "Imagen seleccionada para la publicación",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Constants.IMAGE_HEIGHT.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            2.dp,
                            PurpleTheme.PrimaryLight,
                            RoundedCornerShape(12.dp)
                        ),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(Constants.LARGE_SPACING.dp))

            Button(
                onClick = {
                    scope.launch {
                        val imageUriString = imageUri?.toString()

                        repository.savePublication(
                            Publication(
                                titulo = titulo.trim(),
                                descripcion = descripcion.trim(),
                                imageUri = imageUriString,
                                timestamp = System.currentTimeMillis(),
                                likes = 0,
                                isLiked = false
                            )
                        )
                        message = "✅ Publicación guardada correctamente"

                        titulo = ""
                        descripcion = ""
                        imageUri = null
                        currentPhotoFile = null

                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PurpleTheme.PrimaryPurple,
                    disabledContainerColor = PurpleTheme.PrimaryLight
                )
            ) {
                Text("Guardar Publicación", color = Color.White)
            }

            Spacer(modifier = Modifier.height(Constants.MEDIUM_SPACING.dp))

            if (message.isNotBlank()) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = PurpleTheme.PrimaryPurple
                )
            }
        }
    }
}

@Composable
fun SearchScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { PublicationRepository(context) }
    val publications by repository.getAllPublications().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(SearchResult(emptyList(), emptyList())) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            isLoading = true
            val results = repository.searchAllContent(searchQuery, publications)
            searchResults = results
            isLoading = false
        } else {
            searchResults = SearchResult(emptyList(), emptyList())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PurpleTheme.BackgroundPurple)
    ) {
        SimpleTopAppBar(
            title = "Buscar",
            onBackClick = onNavigateBack
        )

        Divider(color = PurpleTheme.PrimaryLight)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Constants.DEFAULT_PADDING.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar publicaciones y usuarios", color = PurpleTheme.PrimaryDark) },
                placeholder = { Text("Buscar por título, descripción, nombre, profesión...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = PurpleTheme.SurfacePurple,
                    unfocusedContainerColor = PurpleTheme.SurfacePurple,
                    focusedBorderColor = PurpleTheme.PrimaryPurple,
                    unfocusedBorderColor = PurpleTheme.PrimaryLight,
                )
            )

            Spacer(modifier = Modifier.height(Constants.MEDIUM_SPACING.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PurpleTheme.PrimaryPurple)
                    }
                }
                searchQuery.isBlank() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Escribe en el buscador para encontrar publicaciones y usuarios",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = PurpleTheme.PrimaryDark
                        )
                    }
                }
                searchResults.publications.isEmpty() && searchResults.users.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se encontraron resultados para \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PurpleTheme.PrimaryDark
                        )
                    }
                }
                else -> {
                    // Mostrar estadísticas de búsqueda
                    Text(
                        text = "Resultados para \"$searchQuery\":",
                        style = MaterialTheme.typography.labelLarge,
                        color = PurpleTheme.PrimaryDark
                    )

                    if (searchResults.publications.isNotEmpty() || searchResults.users.isNotEmpty()) {
                        Text(
                            text = "${searchResults.publications.size} publicaciones • ${searchResults.users.size} usuarios",
                            style = MaterialTheme.typography.bodySmall,
                            color = PurpleTheme.PrimaryPurple
                        )
                    }

                    Spacer(modifier = Modifier.height(Constants.MEDIUM_SPACING.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Constants.MEDIUM_SPACING.dp)
                    ) {
                        // Mostrar usuarios primero
                        if (searchResults.users.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Usuarios (${searchResults.users.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PurpleTheme.PrimaryDark
                                )
                            }
                            items(searchResults.users) { user ->
                                UserCard(user = user)
                            }

                            // Separador
                            item {
                                Spacer(modifier = Modifier.height(Constants.LARGE_SPACING.dp))
                            }
                        }

                        // Mostrar publicaciones después
                        if (searchResults.publications.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Publicaciones (${searchResults.publications.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PurpleTheme.PrimaryDark
                                )
                            }
                            items(searchResults.publications) { publication ->
                                PublicationCardModern(
                                    publication = publication,
                                    onDelete = {
                                        scope.launch {
                                            repository.deletePublication(publication)
                                        }
                                    },
                                    onLike = { liked ->
                                        scope.launch {
                                            repository.updatePublicationLike(publication.timestamp, liked)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =======================================
// ===== PANTALLAS MEJORADAS =============
// =======================================

// ==================== PANTALLA CONFIGURACIÓN MEJORADA ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionScreen(navController: NavController) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var isDarkMode by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("Español") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PurpleTheme.BackgroundPurple)
    ) {
        SimpleTopAppBar(
            title = "Configuración",
            onBackClick = { navController.popBackStack() }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // MODO OSCURO
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Modo Oscuro",
                    style = MaterialTheme.typography.bodyLarge,
                    color = PurpleTheme.PrimaryDark
                )
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { isDarkMode = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PurpleTheme.PrimaryPurple,
                        uncheckedThumbColor = PurpleTheme.PrimaryLight,
                        uncheckedTrackColor = PurpleTheme.SurfacePurple
                    )
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = PurpleTheme.PrimaryLight
            )

            // IDIOMA
            Text(
                "Idioma",
                style = MaterialTheme.typography.bodyLarge,
                color = PurpleTheme.PrimaryDark
            )
            Spacer(Modifier.height(8.dp))
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    readOnly = true,
                    value = selectedLanguage,
                    onValueChange = { },
                    label = { Text("Seleccionar idioma") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = PurpleTheme.SurfacePurple,
                        unfocusedContainerColor = PurpleTheme.SurfacePurple,
                        focusedIndicatorColor = PurpleTheme.PrimaryPurple,
                        unfocusedIndicatorColor = PurpleTheme.PrimaryLight,
                    ),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    listOf("Español", "English").forEach { lang ->
                        DropdownMenuItem(
                            text = { Text(lang, color = PurpleTheme.PrimaryDark) },
                            onClick = {
                                selectedLanguage = lang
                                expanded = false
                            }
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = PurpleTheme.PrimaryLight
            )

            // NOTIFICACIONES
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Notificaciones",
                    style = MaterialTheme.typography.bodyLarge,
                    color = PurpleTheme.PrimaryDark
                )
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PurpleTheme.PrimaryPurple,
                        uncheckedThumbColor = PurpleTheme.PrimaryLight,
                        uncheckedTrackColor = PurpleTheme.SurfacePurple
                    )
                )
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PurpleTheme.PrimaryPurple
                )
            ) {
                Text("Volver a inicio", color = Color.White)
            }
        }
    }
}

// ==================== PANTALLA CONTÁCTANOS MEJORADA ====================
@Composable
fun ContactanosScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PurpleTheme.BackgroundPurple)
    ) {
        SimpleTopAppBar(
            title = "Contáctanos",
            onBackClick = { navController.popBackStack() }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
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
            tint = PurpleTheme.PrimaryPurple,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = PurpleTheme.PrimaryDark
        )
    }
}

// =======================================
// ====== NUEVAS PANTALLAS MEJORADAS =====
// =======================================

// ==================== PANTALLA PERFIL MEJORADA ====================
@Composable
fun SimpleProfileScreen(onBack: () -> Unit) {
    // Lista de publicaciones específicas del perfil
    val publications = remember {
        mutableStateListOf(
            Publication(
                titulo = "Rafael",
                descripcion = "Buen trabajo Pedro! Sigue trabajando en eso!",
                timestamp = System.currentTimeMillis() - (2 * 60 * 60 * 1000)
            ),
            Publication(
                titulo = "Milenium Oficial",
                descripcion = "Repost by @Centro\n\ntexto de ejemplo xd",
                timestamp = System.currentTimeMillis() - (3 * 60 * 60 * 1000)
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PurpleTheme.BackgroundPurple),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PurpleTheme.LightPurpleGradient)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = PurpleTheme.PrimaryDark,
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
                                color = PurpleTheme.PrimaryDark
                            )
                            Text(
                                text = "Pantera Osorio",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = PurpleTheme.TextPurple
                            )
                            Text(
                                text = "Ing. Sistemas Informáticos e\nInteligencia Artificial",
                                fontSize = 14.sp,
                                color = PurpleTheme.TextPurple,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Redes",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = PurpleTheme.PrimaryDark
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
                                    .border(2.dp, PurpleTheme.PrimaryLight, CircleShape)
                                    .background(PurpleTheme.SurfacePurple),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccountCircle,
                                    contentDescription = "Foto de perfil",
                                    modifier = Modifier.size(80.dp),
                                    tint = PurpleTheme.PrimaryPurple
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${publications.size}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = PurpleTheme.PrimaryDark
                            )
                            Text(
                                text = "Posts",
                                fontSize = 12.sp,
                                color = PurpleTheme.TextPurple
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
                    color = PurpleTheme.PrimaryPurple
                )
                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = PurpleTheme.PrimaryLight, thickness = 1.dp)
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
                        val updatedPublication = publication.copy(isLiked = liked)
                        publications[index] = updatedPublication
                    }
                }
            )
        }
    }
}

// ==================== PANTALLA NOTIFICACIONES MEJORADA ====================
@Composable
fun NotificationScreenStyle(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PurpleTheme.BackgroundPurple)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PurpleTheme.PrimaryPurple)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "Atrás",
                    tint = Color.White
                )
            }

            Text(
                text = "Notificaciones",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Icon(
                Icons.Filled.Search,
                contentDescription = "Buscar",
                tint = Color.White
            )
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
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(PurpleTheme.SurfacePurple, RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.AccountCircle,
            contentDescription = null,
            modifier = Modifier.size(50.dp),
            tint = PurpleTheme.PrimaryPurple
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = names.getOrElse(index) { "Usuario" },
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = PurpleTheme.PrimaryDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLike) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = PurpleTheme.PrimaryPurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = actions.getOrElse(index) { "" },
                    color = PurpleTheme.TextPurple,
                    fontSize = 14.sp
                )
            }
        }

        Text(
            text = times.getOrElse(index) { "" },
            color = PurpleTheme.PrimaryPurple,
            fontSize = 12.sp
        )
    }
}

// =======================================
// ============= UTILITIES ===============
// =======================================

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy 'a las' HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// =======================================
// ============= PREVIEWS ================
// =======================================

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MaterialTheme(
        colorScheme = PurpleColorScheme
    ) {
        HomeScreen(navController = rememberNavController())
    }
}

@Preview(showBackground = true)
@Composable
fun CreateScreenPreview() {
    MaterialTheme(
        colorScheme = PurpleColorScheme
    ) {
        CreateScreen(
            navController = rememberNavController(),
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SearchScreenPreview() {
    MaterialTheme(
        colorScheme = PurpleColorScheme
    ) {
        SearchScreen(onNavigateBack = {})
    }
}

@Preview(showBackground = true)
@Composable
fun PublicationCardPreview() {
    MaterialTheme(
        colorScheme = PurpleColorScheme
    ) {
        PublicationCardModern(
            publication = Publication(
                titulo = "Mi primera publicación",
                descripcion = "Esta es una descripción de ejemplo para mostrar cómo se vería una publicación en la aplicación.",
                imageUri = null,
                timestamp = System.currentTimeMillis(),
                likes = 5,
                isLiked = true
            ),
            onDelete = { },
            onLike = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun UserCardPreview() {
    MaterialTheme(
        colorScheme = PurpleColorScheme
    ) {
        UserCard(
            user = User(
                id = 1,
                nombre = "María González",
                edad = 25,
                profesion = "Diseñadora UX",
                descripcion = "Apasionada por el diseño y la tecnología.",
                intereses = listOf("Diseño", "Tecnología", "Arte")
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ConfiguracionScreenPreview() {
    MaterialTheme(
        colorScheme = PurpleColorScheme
    ) {
        ConfiguracionScreen(navController = rememberNavController())
    }
}

@Preview(showBackground = true)
@Composable
fun ContactanosScreenPreview() {
    MaterialTheme(
        colorScheme = PurpleColorScheme
    ) {
        ContactanosScreen(navController = rememberNavController())
    }
}

@Preview(showBackground = true)
@Composable
fun SimpleProfileScreenPreview() {
    MaterialTheme(
        colorScheme = PurpleColorScheme
    ) {
        SimpleProfileScreen(onBack = {})
    }
}

@Preview(showBackground = true)
@Composable
fun NotificationScreenStylePreview() {
    MaterialTheme(
        colorScheme = PurpleColorScheme
    ) {
        NotificationScreenStyle(onBack = {})
    }
}