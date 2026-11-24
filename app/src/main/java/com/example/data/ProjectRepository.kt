package com.example.data

import android.content.Context
import com.example.R
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.AppProject
import com.example.model.AppScreen
import com.example.model.AppThemePreset
import com.example.model.ArchitecturePattern
import com.example.model.BackendSpec
import com.example.model.DeploymentRecord
import com.example.model.DeploymentStatus
import com.example.model.ProjectFile
import com.example.model.ScalingTier
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ProjectRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    fun observeProjects(userId: String): Flow<List<AppProject>> {
        val path = "users/$userId/projects"
        return db.collection("users")
            .document(userId)
            .collection("projects")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc -> docToProject(doc) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeDeployments(userId: String): Flow<List<DeploymentRecord>> {
        val path = "users/$userId/deployments"
        return db.collection("users")
            .document(userId)
            .collection("deployments")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc -> docToDeployment(doc) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun createProject(userId: String, project: AppProject): Result<String> {
        var docPath = "users/$userId/projects"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            val docRef = db.collection("users").document(uid).collection("projects").document()
            docPath = docRef.path
            val sanitized = project.copy(
                userId = uid,
                name = project.name.trim().take(80).ifBlank { "Untitled App" },
                packageName = project.packageName.trim().take(120).ifBlank { "com.forge.app" },
                tagline = project.tagline.trim().take(180)
            )
            docRef.set(sanitized.toCreateMap(uid)).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateProjectDesign(userId: String, project: AppProject): Result<Unit> {
        val projectId = project.id
        var docPath = "users/$userId/projects/$projectId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            require(projectId.isNotBlank()) { "Project ID must not be blank for update." }
            val docRef = db.collection("users").document(uid).collection("projects").document(projectId)
            docPath = docRef.path
            docRef.update(project.toDesignUpdateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateProjectCloud(userId: String, project: AppProject): Result<Unit> {
        val projectId = project.id
        var docPath = "users/$userId/projects/$projectId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            require(projectId.isNotBlank()) { "Project ID must not be blank for cloud update." }
            val docRef = db.collection("users").document(uid).collection("projects").document(projectId)
            docPath = docRef.path
            docRef.update(project.toCloudUpdateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteProject(userId: String, projectId: String): Result<Unit> {
        var docPath = "users/$userId/projects/$projectId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user delete." }
            require(projectId.isNotBlank()) { "Project ID must not be blank for delete." }
            val docRef = db.collection("users").document(uid).collection("projects").document(projectId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deployRelease(
        userId: String,
        project: AppProject,
        environment: String,
        versionTag: String,
        summary: String
    ): Result<String> {
        var docPath = "users/$userId/deployments"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user deployment." }
            require(project.id.isNotBlank()) { "Project ID must not be blank." }
            val safeEnv = if (environment == "STAGING") "STAGING" else "PRODUCTION"
            val nextStatus = if (safeEnv == "PRODUCTION") DeploymentStatus.DEPLOYED else DeploymentStatus.STAGING
            val slug = project.name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "app" }
            val endpointUrl = "https://$slug-${project.cloudRegion}.forgestudio.run.app/$versionTag"

            // 1. Update project cloud status
            val updatedProject = project.copy(
                deploymentStatus = nextStatus,
                activeVersion = versionTag.take(24).ifBlank { "v1.0.1" }
            )
            val projRef = db.collection("users").document(uid).collection("projects").document(project.id)
            projRef.update(updatedProject.toCloudUpdateMap()).await()

            // 2. Record immutable deployment log
            val depRef = db.collection("users").document(uid).collection("deployments").document()
            docPath = depRef.path
            val payload = mapOf(
                "userId" to uid,
                "projectId" to project.id,
                "projectName" to project.name.take(80).ifBlank { "App" },
                "versionTag" to versionTag.take(24).ifBlank { "v1.0.1" },
                "environment" to safeEnv,
                "cloudRegion" to project.cloudRegion.take(40).ifBlank { "asia-southeast1" },
                "status" to "SUCCEEDED",
                "endpointUrl" to endpointUrl.take(240),
                "summary" to summary.take(500),
                "createdAt" to FieldValue.serverTimestamp()
            )
            depRef.set(payload).await()
            Result.success(endpointUrl)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun rollbackDeployment(userId: String, deploymentId: String): Result<Unit> {
        var docPath = "users/$userId/deployments/$deploymentId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user rollback." }
            require(deploymentId.isNotBlank()) { "Deployment ID must not be blank." }
            val depRef = db.collection("users").document(uid).collection("deployments").document(deploymentId)
            docPath = depRef.path
            depRef.update(
                mapOf(
                    "status" to "ROLLED_BACK",
                    "summary" to "Deployment rolled back by owner."
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    private fun docToProject(doc: DocumentSnapshot): AppProject? {
        val data = doc.data ?: return null
        val rawScreens = (data["screens"] as? List<*>) ?: emptyList<Any>()
        val rawFiles = (data["files"] as? List<*>) ?: emptyList<Any>()
        val rawBackend = data["backend"] as? Map<*, *>

        return AppProject(
            id = doc.id,
            userId = (data["userId"] as? String).orEmpty(),
            name = (data["name"] as? String).orEmpty(),
            packageName = (data["packageName"] as? String).orEmpty(),
            tagline = (data["tagline"] as? String).orEmpty(),
            themePreset = AppThemePreset.safeValueOf(data["themePreset"] as? String),
            architecture = ArchitecturePattern.safeValueOf(data["architecture"] as? String),
            cloudRegion = (data["cloudRegion"] as? String).orEmpty().ifBlank { "asia-southeast1" },
            scalingTier = ScalingTier.safeValueOf(data["scalingTier"] as? String),
            deploymentStatus = DeploymentStatus.safeValueOf(data["deploymentStatus"] as? String),
            activeVersion = (data["activeVersion"] as? String).orEmpty().ifBlank { "v1.0.0" },
            screens = rawScreens.mapNotNull { (it as? Map<*, *>)?.let(AppScreen::fromMap) },
            files = rawFiles.mapNotNull { (it as? Map<*, *>)?.let(ProjectFile::fromMap) },
            backend = BackendSpec.fromMap(rawBackend),
            createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
            updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }

    private fun docToDeployment(doc: DocumentSnapshot): DeploymentRecord? {
        val data = doc.data ?: return null
        return DeploymentRecord(
            id = doc.id,
            userId = (data["userId"] as? String).orEmpty(),
            projectId = (data["projectId"] as? String).orEmpty(),
            projectName = (data["projectName"] as? String).orEmpty(),
            versionTag = (data["versionTag"] as? String).orEmpty(),
            environment = (data["environment"] as? String).orEmpty().ifBlank { "PRODUCTION" },
            cloudRegion = (data["cloudRegion"] as? String).orEmpty().ifBlank { "asia-southeast1" },
            status = (data["status"] as? String).orEmpty().ifBlank { "SUCCEEDED" },
            endpointUrl = (data["endpointUrl"] as? String).orEmpty(),
            summary = (data["summary"] as? String).orEmpty(),
            createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: Timestamp.now()
        )
    }
}
