package laplacian_arch.service_api_arch
import com.github.jknack.handlebars.Context
import laplacian.gradle.task.generate.ExecutionContext
import laplacian.gradle.task.generate.ProjectEntryResolver
import laplacian_arch.service_api_arch.gradle.ServiceApiArchModelEntryResolver

import laplacian_arch.service_api_arch.model.DatasourceList

import laplacian_arch.service_api_arch.model.ServiceList

import laplacian_arch.service_api_arch.model.RestResourceList

import laplacian_arch.service_api_arch.model.DataAccessList

import laplacian.util.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors

/**
 * A test utility class which asserts the content of the files generated by with a template.
 */
class ServiceApiArchTemplateAssertion {
    lateinit var context: ExecutionContext
    lateinit var datasources: DatasourceList
    lateinit var services: ServiceList
    lateinit var restResources: RestResourceList
    lateinit var dataAccesses: DataAccessList

    /**
     * Reads the model objects from the given yaml files.
     */
    fun withModel(files: List<File>): ServiceApiArchTemplateAssertion {
        context = ExecutionContext().apply {
            val projectModel = File("laplacian-module.yml")
            if (projectModel.exists()) modelFiles.add(projectModel)
            modelFiles.addAll(files)
            modelEntryResolvers.addAll(listOf(
                ProjectEntryResolver(),
                ServiceApiArchModelEntryResolver()
            ))
        }
        context.build()
        datasources = context.currentModel.get("datasources") as DatasourceList
        services = context.currentModel.get("services") as ServiceList
        restResources = context.currentModel.get("rest_resources") as RestResourceList
        dataAccesses = context.currentModel.get("data_accesses") as DataAccessList
        return this
    }

    /**
     * Reads the model objects from the given yaml files.
     */
    fun withModel(path: String, glob: String = "{*,**/*}.yml"): ServiceApiArchTemplateAssertion {
        val m = FileSystems.getDefault().getPathMatcher("glob:$path/$glob")
        val files = Files.walk(Paths.get(path))
                   .filter { m.matches(it) }
                   .map{ it.toFile() }
                   .collect(Collectors.toList())
        withModel(files)
        return this
    }

    /**
     * Reads the model objects from the given yaml expression.
     */
    fun withModelText(text: String): ServiceApiArchTemplateAssertion {
        val temp = File.createTempFile(ServiceApiArchTemplateAssertion::javaClass.name, ".yml")
        temp.deleteOnExit()
        temp.writeText(text)
        withModel(listOf(temp))
        return this
    }

    lateinit var template: File

    /**
     * Sets the test target template file.
     */
    fun withTemplate(templatePath: String): ServiceApiArchTemplateAssertion {
        template = File(templatePath)
        return this
    }

    /**
     * Asserts that the given file and the generated one have the same content.
     */
    fun assertSameContent(toBeSourceCodePath: String, handleModel:ServiceApiArchTemplateAssertion.() -> Map<String, Any?> = { emptyMap() }) {
        val child = handleModel.invoke(this)
        val ctx = Context.newContext(context.currentModel, child)
        val actual = template.readText().handlebars().apply(ctx)
        val expect = File(toBeSourceCodePath).readText()
        assertEquals(expect.stripDocComments(), actual.stripDocComments())
    }

    /**
     * Asserts that the generated file from the given template includes the certain content.
     */
    fun assertContains(content: String, handleModel:ServiceApiArchTemplateAssertion.() -> Map<String, Any?> = { emptyMap() }) {
        val child = handleModel.invoke(this)
        val ctx = Context.newContext(context.currentModel, child)
        val actual = template.readText().handlebars().apply(ctx).stripDocComments()
        val expect = content.stripDocComments()
        val result = actual.replace("""(\n|^)\s*""".toRegex(), "\n").contains(expect.replace("""(\n|^)\s*""".toRegex(), "\n"))
        assertTrue(result, "\n---\n$expect\n---\nThe above content should be included in the following generated code:\n---\n${actual}")
    }
}