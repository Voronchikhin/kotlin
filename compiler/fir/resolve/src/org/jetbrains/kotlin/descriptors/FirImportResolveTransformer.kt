/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.UnambiguousFqName
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedImportImpl
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.name.FqName

class FirImportResolveTransformer : FirTransformer<Nothing?>() {
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        return file.also { it.transformChildren(this, null) }.compose()
    }

    override fun transformImport(import: FirImport, data: Nothing?): CompositeTransformResult<FirImport> {
        val fqName = import.importedFqName ?: return import.compose()
        val firProvider = FirProvider.getInstance(import.session)

        if (!fqName.isRoot) {
            val lastPart = mutableListOf<String>()
            var firstPart = fqName

            while (!firstPart.isRoot) {
                lastPart.add(0, firstPart.shortName().asString())
                firstPart = firstPart.parent()

                val resolvedFqName = UnambiguousFqName(firstPart.toUnsafe(), FqName.fromSegments(lastPart))
                val foundClassifier = firProvider.getFirClassifierByFqName(resolvedFqName)

                if (foundClassifier != null) {
                    return FirResolvedImportImpl(import, resolvedFqName).compose()
                }
            }
            return import.compose()
        } else {
            return import.compose()
        }
    }
}