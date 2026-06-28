/*
 * Copyright (c) 2026 Oscar Spruit
 *
 * This file is open source and available under the Apache 2.0 license. See the LICENSE file for more info.
 */

package dev.oscarspruit.unsealed.idea

import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModCommand
import com.intellij.modcommand.Presentation
import com.intellij.modcommand.PsiBasedModCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.Key
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.Processor
import dev.oscarspruit.unsealed.compiler.UnsealedBranchAnalysis
import dev.oscarspruit.unsealed.compiler.UnsealedClassIds
import dev.oscarspruit.unsealed.compiler.UnsealedTreeRegistry
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtWhenConditionIsPattern
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.resolve.ImportPath
import java.io.File

private val TREE_REGISTRY_KEY: Key<CachedValue<UnsealedTreeRegistry>> = Key.create("unsealed.treeRegistry")

internal class AddMissingUnsealedBranchesAction :
    PsiBasedModCommandAction<KtWhenExpression>(KtWhenExpression::class.java) {

    override fun getFamilyName(): String = "Unsealed"

    override fun getPresentation(context: ActionContext, element: KtWhenExpression): Presentation? {
        val hasUnsealedRoot = analyze(element) {
            val subjectExpr = element.subjectExpression ?: return@analyze false
            val subjectType = subjectExpr.expressionType ?: return@analyze false
            val classSymbol = subjectType.symbol as? KaClassSymbol ?: return@analyze false
            classSymbol.annotations.any { it.classId == UnsealedClassIds.UNSEALED_ROOT }
        }
        if (!hasUnsealedRoot) return null
        return Presentation.of("Add remaining branches")
    }

    override fun perform(context: ActionContext, element: KtWhenExpression): ModCommand {
        val missingBranches = getMissingBranches(context.project, element)
        if (missingBranches.isEmpty()) return ModCommand.nop()

        return ModCommand.psiUpdate(element) { whenExpr, _ ->
            val factory = KtPsiFactory(whenExpr.project)
            val closeBrace = whenExpr.closeBrace ?: return@psiUpdate
            val file = whenExpr.containingKtFile

            for (classId in missingBranches) {
                val simpleName = classId.shortClassName.asString()
                val entry = factory.createWhenEntry("is $simpleName -> TODO()")
                whenExpr.addBefore(entry, closeBrace)
                whenExpr.addBefore(factory.createNewLine(), closeBrace)
            }

            addMissingImports(factory, file, missingBranches)
        }
    }

    // org.jetbrains.kotlin.resolve is a K1 API and is scheduled for removal, but K2 doesn't have a
    // replacement yet
    @Suppress("UnstableApiUsage")
    private fun addMissingImports(
        factory: KtPsiFactory,
        file: KtFile,
        missingBranches: Set<ClassId>,
    ) {
        val importList = file.importList ?: return
        val existingImports = file.importDirectives.mapNotNull { it.importPath }.toSet()

        val newImports = missingBranches
            .filter { it.packageFqName != file.packageFqName }
            .map { ImportPath(it.asSingleFqName(), false) }
            .filter { it !in existingImports }

        for (importPath in newImports) {
            val newDirective = factory.createImportDirective(importPath)
            val importPathString = importPath.toString()
            val anchor = importList.imports.firstOrNull {
                it.importPath.toString() > importPathString
            }
            if (anchor != null) {
                importList.addBefore(newDirective, anchor)
            } else {
                importList.add(newDirective)
            }
        }
    }

    private fun getMissingBranches(project: Project, whenExpr: KtWhenExpression): Set<ClassId> {
        val analysis = analyzeWhenExpression(whenExpr) ?: return emptySet()

        val treeRegistry = getTreeRegistry(project)
        val dependencyLeaves = treeRegistry.getLeavesForRoot(analysis.rootClassId)
        val sourceLeaves = findSourceLeaves(project, analysis.rootClassId)
        val allLeaves = dependencyLeaves + sourceLeaves
        if (allLeaves.isEmpty()) return emptySet()

        return UnsealedBranchAnalysis.findMissingBranches(allLeaves, analysis.coveredBranches, analysis.hasElseBranch)
    }

    private fun analyzeWhenExpression(whenExpr: KtWhenExpression): WhenAnalysis? = analyze(whenExpr) {
        val subjectExpr = whenExpr.subjectExpression ?: return@analyze null
        val subjectType = subjectExpr.expressionType ?: return@analyze null
        val classSymbol = subjectType.symbol as? KaClassSymbol ?: return@analyze null

        val hasUnsealedRoot = classSymbol.annotations.any { annotation ->
            annotation.classId == UnsealedClassIds.UNSEALED_ROOT
        }
        if (!hasUnsealedRoot) return@analyze null

        val rootClassId = classSymbol.classId ?: return@analyze null

        val coveredBranches = whenExpr.entries
            .flatMap { it.conditions.toList() }
            .filterIsInstance<KtWhenConditionIsPattern>()
            .mapNotNull { condition ->
                val typeRef = condition.typeReference ?: return@mapNotNull null
                val type = typeRef.type
                (type.symbol as? KaClassSymbol)?.classId
            }
            .toSet()

        val hasElseBranch = whenExpr.entries.any { it.isElse }

        WhenAnalysis(rootClassId, coveredBranches, hasElseBranch)
    }

    private fun getTreeRegistry(project: Project): UnsealedTreeRegistry {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            TREE_REGISTRY_KEY,
            { createTreeRegistryResult(project) },
            false,
        )
    }

    private fun createTreeRegistryResult(project: Project): CachedValueProvider.Result<UnsealedTreeRegistry> {
        val classpathEntries = ProjectRootManager.getInstance(project)
            .orderEntries()
            .classesRoots
            .mapNotNull { vf -> vf.canonicalPath?.let { File(it.removeSuffix("!/")) } }
        return CachedValueProvider.Result.create(
            UnsealedTreeRegistry(classpathEntries),
            ProjectRootModificationTracker.getInstance(project),
        )
    }

    private fun findSourceLeaves(project: Project, rootClassId: ClassId): Set<ClassId> {
        val projectScope = GlobalSearchScope.projectScope(project)
        val allScope = GlobalSearchScope.allScope(project)

        val unsealedLeafFqn = UnsealedClassIds.UNSEALED_LEAF.asSingleFqName().asString()
        val annotationPsiClass = JavaPsiFacade.getInstance(project)
            .findClass(unsealedLeafFqn, allScope)
            ?: return emptySet()

        val rootFqn = rootClassId.asSingleFqName().asString()
        val rootPsiClass = JavaPsiFacade.getInstance(project)
            .findClass(rootFqn, allScope)
            ?: return emptySet()

        val result = mutableSetOf<ClassId>()
        AnnotatedElementsSearch.searchPsiClasses(annotationPsiClass, projectScope)
            .forEach(
                Processor { psiClass ->
                    if (psiClass.isInheritor(rootPsiClass, true)) {
                        val classId = UnsealedClassIds.classIdFromFqn(psiClass.qualifiedName ?: return@Processor true)
                        if (classId != null) result.add(classId)
                    }
                    true
                },
            )
        return result
    }

    private data class WhenAnalysis(
        val rootClassId: ClassId,
        val coveredBranches: Set<ClassId>,
        val hasElseBranch: Boolean,
    )
}
