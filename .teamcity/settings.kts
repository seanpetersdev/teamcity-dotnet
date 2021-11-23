import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.ReSharperDuplicates
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.dotnetBuild
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.reSharperDuplicates
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.projectFeatures.githubIssues
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2021.2"

project {
    description = "Example .net project"

    buildType(Compile)

    features {
        githubIssues {
            id = "PROJECT_EXT_2"
            displayName = "infojolt/teamcity-dotnet"
            repositoryURL = "https://github.com/infojolt/teamcity-dotnet"
        }
    }
}

object Compile : BuildType({
    name = "Compile"

    artifactRules = "TeamCityDotNet/bin/Debug/netcoreapp3.1 => TeamCityDotNet.zip"

    params {
        param("BuildApiEndpoint", "http://teamcity-server:8111/app/rest/buildQueue")
        password("BearerToken", "credentialsJSON:be2340ee-fa95-4882-8491-4013dfaa46e1")
        param("BuildConfigurationId", "DotnetHelloWorld_Compile")
        param("URL", "https://blahblah.com")
        param("env.RELEASE_NUMBER", "")
        param("env.SESSION_TOKEN", "test")
    }

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        dotnetBuild {
            name = "Build Solution"
            projects = "TeamCityDotNet.sln"
            param("dotNetCoverage.dotCover.home.path", "%teamcity.tool.JetBrains.dotCover.CommandLineTools.DEFAULT%")
        }
        reSharperDuplicates {
            name = "Dupe check"
            cltPath = "%teamcity.tool.jetbrains.resharper-clt.DEFAULT%"
            cltPlatform = ReSharperDuplicates.Platform.CROSS_PLATFORM
        }
        script {
            name = "call rest api with curl"
            enabled = false
            scriptContent = """curl -d '{"buildType": {"id": "DotnetHelloWorld_Compile"}}' -H "Content-Type: application/json" -H "Authorization: Bearer eyJ0eXAiOiAiVENWMiJ9.c0pDUlhHb05maWppOU0yTUpSY0ZoeFRITkdB.NWI2YjljYWMtOTBiYy00NDIyLTg2YzctMDkzNjg3MGE3NTJh" -X POST http://teamcity-server:8111/app/rest/buildQueue"""
        }
        script {
            name = "Check version is set"
            scriptContent = """
                #!/bin/bash
                if [[ -z ${'$'}RELEASE_NUMBER ]]; then
                    >&2 echo "Deployment version is not set"
                    exit 1
                fi
                if [[ -z ${'$'}SESSION_TOKEN ]]; then
                    >&2 echo "SESSION_TOKEN params not set"
                    exit 1
                fi
                #if [[ -z ${'$'}URL ]]; then
                #    >&2 echo "URL not set"
                #    exit 1
                #fi
                echo "##teamcity[setParameter name='env.RELEASE_NUMBER' value='${'$'}RELEASE_NUMBER']"
            """.trimIndent()
            formatStderrAsError = true
        }
        script {
            name = "call REST API using bash"
            enabled = false
            scriptContent = """
                #!/bin/bash
                curl -d '{"buildType": {"id": "DotnetHelloWorld_Compile"}}' -H "Content-Type: application/json" -H "Authorization: Bearer eyJ0eXAiOiAiVENWMiJ9.c0pDUlhHb05maWppOU0yTUpSY0ZoeFRITkdB.NWI2YjljYWMtOTBiYy00NDIyLTg2YzctMDkzNjg3MGE3NTJh" -X POST http://teamcity-server:8111/app/rest/buildQueue
                echo "##teamcity[setParameter name='env.RELEASE_NUMBER' value='${'$'}RELEASE_NUMBER']"
            """.trimIndent()
        }
        script {
            name = "call REST API with params"
            enabled = false
            scriptContent = """
                #!/bin/bash
                curl -d '{"buildType":{"id": "DotnetHelloWorld_Compile"},"properties": {"property": [{ "name": "env.RELEASE_NUMBER", "value": "v1.0.%build.counter%"}]}}' -H "Content-Type: application/json" -H "Authorization: Bearer eyJ0eXAiOiAiVENWMiJ9.c0pDUlhHb05maWppOU0yTUpSY0ZoeFRITkdB.NWI2YjljYWMtOTBiYy00NDIyLTg2YzctMDkzNjg3MGE3NTJh" -X POST http://teamcity-server:8111/app/rest/buildQueue
                echo "##teamcity[setParameter name='env.RELEASE_NUMBER' value='${'$'}RELEASE_NUMBER']"
            """.trimIndent()
        }
        script {
            name = "call REST API with params and vars (1)"
            scriptContent = """
                #!/bin/bash
                curl -d '{"buildType":{"id": "%BuildConfigurationId%"},"properties": {"property": [{ "name": "env.RELEASE_NUMBER", "value": "v1.0.%build.counter%"}]}}' -H "Content-Type: application/json" -H "Authorization: Bearer %BearerToken%" -X POST %BuildApiEndpoint%
            """.trimIndent()
        }
        script {
            name = "call REST API with params and vars"
            enabled = false
            scriptContent = """
                #!/bin/bash
                curl -d '{"buildType":{"id": "%BuildConfigurationId%"},"properties": {"property": [{ "name": "env.RELEASE_NUMBER", "value": "v1.0.%build.counter%"}]}}' -H "Content-Type: application/json" -H "Authorization: Bearer %BearerToken%" -X POST %BuildApiEndpoint%
            """.trimIndent()
        }
    }

    triggers {
        vcs {
            branchFilter = ""
            perCheckinTriggering = true
            groupCheckinsByCommitter = true
            enableQueueOptimization = false
        }
    }
})
