package com.example.milenium

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
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
        composable("inicio") {
            PantallaInicio(navController = navController)
        }
        composable("perfil") {
            PantallaBlanca(titulo = "Perfil")
        }
        composable("notificaciones") {
            PantallaBlanca(titulo = "Notificaciones")
        }
        composable("contactanos") {
            PantallaBlanca(titulo = "Contáctanos")
        }
        composable("configuracion") {
            PantallaBlanca(titulo = "Configuración")
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
            ModalDrawerSheet {
                Text(
                    text = "Menú",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Inicio") },
                    selected = false,
                    onClick = {
                        navController.navigate("inicio")
                        scopeDrawer.launch { drawerState.close() }
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Publicaciones") },
                    selected = true,
                    onClick = {
                        navController.navigate("publicaciones")
                        scopeDrawer.launch { drawerState.close() }
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Perfil") },
                    selected = false,
                    onClick = {
                        navController.navigate("perfil")
                        scopeDrawer.launch { drawerState.close() }
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Notificaciones") },
                    selected = false,
                    onClick = {
                        navController.navigate("notificaciones")
                        scopeDrawer.launch { drawerState.close() }
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Contáctanos") },
                    selected = false,
                    onClick = {
                        navController.navigate("contactanos")
                        scopeDrawer.launch { drawerState.close() }
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Configuración") },
                    selected = false,
                    onClick = {
                        navController.navigate("configuracion")
                        scopeDrawer.launch { drawerState.close() }
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
                            scopeDrawer.launch { drawerState.open() }
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
                                navController.navigate("publicaciones")
                            }
                        )
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate("buscar") }) {
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
                    onClick = { navController.navigate("crear_publicacion") }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Crear nueva publicación"
                    )
                }
            },

            floatingActionButtonPosition = FabPosition.End
        ) { paddingValues ->
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
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(Constants.MEDIUM_SPACING.dp))
                        Text(
                            text = "No hay publicaciones",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(Constants.SMALL_SPACING.dp))
                        Text(
                            text = "Haz clic en el botón + para crear tu primera publicación",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
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
            title = { Text("Eliminar Publicación") },
            text = { Text("¿Estás seguro de que quieres eliminar esta publicación? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "U",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Usuario",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formatDate(publication.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Contenido
            Text(
                text = publication.titulo,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = publication.descripcion,
                style = MaterialTheme.typography.bodyMedium
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
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } ?: run {
                if (!publication.imageUri.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(Constants.MEDIUM_SPACING.dp))
                    Text(
                        text = "⚠️ Imagen no disponible",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
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
                        tint = if (isLiked) Color(0xFFFF5252) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${publication.likes + if (isLiked && !publication.isLiked) 1 else if (!isLiked && publication.isLiked) -1 else 0}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isLiked) Color(0xFFFF5252) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Compartir",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

// =======================================
// ======= PANTALLA INICIO SECUNDARIA ====
// =======================================

@Composable
fun PantallaInicio(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Bienvenido a Milenium",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Esta es la pantalla de inicio secundaria",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { navController.navigate("publicaciones") }
            ) {
                Text("Ir a Publicaciones")
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
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constants.DEFAULT_PADDING.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar del usuario
            Surface(
                modifier = Modifier.size(60.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.nombre.take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
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
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${user.edad} años • ${user.profesion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = user.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Intereses
                if (user.intereses.isNotEmpty()) {
                    Text(
                        text = "Intereses: ${user.intereses.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
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

    Column(modifier = Modifier.fillMaxSize()) {
        SimpleTopAppBar(
            title = "Perfil de Usuario",
            onBackClick = { /* Manejar navegación back */ }
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (user == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Usuario no encontrado")
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
            .padding(horizontal = Constants.DEFAULT_PADDING.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (onBackClick != null) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver"
                )
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
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

    Column(modifier = Modifier.fillMaxSize()) {
        SimpleTopAppBar(
            title = "Crear Publicación",
            onBackClick = onNavigateBack
        )

        Divider()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Constants.DEFAULT_PADDING.dp)
        ) {
            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it },
                label = { Text("Título") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(Constants.MEDIUM_SPACING.dp))

            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(Constants.MEDIUM_SPACING.dp))

            Text(
                text = "Seleccionar imagen:",
                style = MaterialTheme.typography.labelLarge
            )

            Spacer(modifier = Modifier.height(Constants.SMALL_SPACING.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = {
                    galleryLauncher.launch("image/*")
                }) {
                    Text("Galería")
                }

                Button(onClick = {
                    val photoFile = imageManager.createImageFile()
                    currentPhotoFile = photoFile
                    val photoUri = imageManager.getPersistentUri(photoFile)
                    cameraLauncher.launch(photoUri)
                }) {
                    Text("Cámara")
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
                        .clip(RoundedCornerShape(12.dp)),
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
                enabled = isFormValid
            ) {
                Text("Guardar Publicación")
            }

            Spacer(modifier = Modifier.height(Constants.MEDIUM_SPACING.dp))

            if (message.isNotBlank()) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
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

    Column(modifier = Modifier.fillMaxSize()) {
        SimpleTopAppBar(
            title = "Buscar",
            onBackClick = onNavigateBack
        )

        Divider()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Constants.DEFAULT_PADDING.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar publicaciones y usuarios") },
                placeholder = { Text("Buscar por título, descripción, nombre, profesión...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
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
                        CircularProgressIndicator()
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
                            textAlign = TextAlign.Center
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
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                else -> {
                    // Mostrar estadísticas de búsqueda
                    Text(
                        text = "Resultados para \"$searchQuery\":",
                        style = MaterialTheme.typography.labelLarge
                    )

                    if (searchResults.publications.isNotEmpty() || searchResults.users.isNotEmpty()) {
                        Text(
                            text = "${searchResults.publications.size} publicaciones • ${searchResults.users.size} usuarios",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
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
                                    fontWeight = FontWeight.Bold
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
                                    fontWeight = FontWeight.Bold
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
    MaterialTheme {
        HomeScreen(navController = rememberNavController())
    }
}

@Preview(showBackground = true)
@Composable
fun CreateScreenPreview() {
    MaterialTheme {
        CreateScreen(
            navController = rememberNavController(),
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SearchScreenPreview() {
    MaterialTheme {
        SearchScreen(onNavigateBack = {})
    }
}

@Preview(showBackground = true)
@Composable
fun PublicationCardPreview() {
    MaterialTheme {
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
    MaterialTheme {
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