import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.PowerShellStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.ReSharperDuplicates
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.dotnetBuild
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.dotnetRun
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.nodeJS
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.powerShell
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
    buildType(PostmanTests)
    buildType(PostmanTestsBoth)
    buildType(PostmanTestsPowershell)

    features {
        githubIssues {
            id = "PROJECT_EXT_2"
            displayName = "infojolt/teamcity-dotnet"
            repositoryURL = "https://github.com/infojolt/teamcity-dotnet"
        }
    }

    subProject(TestSubProject)
}

object Compile : BuildType({
    name = "Compile"

    artifactRules = "TeamCityDotNet/bin/Debug/netcoreapp3.1 => TeamCityDotNet.zip"

    params {
        param("teamcity.build_queue_endpoint", "/app/rest/buildQueue")
        param("teamcity.auth_endpoint", "/authenticationTest.html?csrf")
        param("env.AWS_SECRET_ACCESS_KEY", "4PxW/vROArn9K6F21KzPxy6fUSPtcqKDS+5MCDmS")
        param("env.RELEASE_NUMBER", "")
        param("env.AWS_ACCESS_KEY_ID", "AKIARU5HXEIKDO6UQV6Y")
        param("teamcity.stage.build_config_id", "DotnetHelloWorld_Compile")
        password("teamcity.stage.auth_token", "credentialsJSON:be2340ee-fa95-4882-8491-4013dfaa46e1")
        param("teamcity.stage.server", "http://teamcity-server:8111")
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
        dotnetRun {
            name = "run it"
            projects = "TeamCityDotNet/TeamCityDotNet.csproj"
            param("dotNetCoverage.dotCover.home.path", "%teamcity.tool.JetBrains.dotCover.CommandLineTools.DEFAULT%")
        }
        script {
            name = "Check version is set"
            scriptContent = """
                #!/bin/bash
                if [[ -z ${'$'}RELEASE_NUMBER ]]; then
                    >&2 echo "Deployment version is not set"
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
            name = "call REST API with params and vars"
            enabled = false
            scriptContent = """
                #!/bin/bash
                curl -d '{"buildType":{"id": "%BuildConfigurationId%"},"properties": {"property": [{ "name": "env.RELEASE_NUMBER", "value": "v1.0.%build.counter%"}]}}' -H "Content-Type: application/json" -H "Authorization: Bearer %BearerToken%" -X POST %BuildApiEndpoint%
            """.trimIndent()
        }
        script {
            name = "call REST API with params and vars and csrf"
            enabled = false
            scriptContent = """
                #!/bin/bash
                RESPONSE=`curl -H "Authorization: Bearer %teamcity.stage.auth_token%" %teamcity.stage.server%%teamcity.auth_endpoint%`
                curl -d '{"buildType":{"id": "%teamcity.stage.build_config_id%"},"properties": {"property": [{ "name": "env.RELEASE_NUMBER", "value": "v1.0.%build.counter%"}]}}' -H "Content-Type: application/json" -H "Authorization: Bearer %teamcity.stage.auth_token%" -H "X-TC-CSRF-Token: ${'$'}RESPONSE" -X POST %teamcity.stage.server%%teamcity.build_queue_endpoint%
            """.trimIndent()
        }
        script {
            name = "Call rest api powershell"
            enabled = false
            scriptContent = """
                ${'$'}AuthHeader = @{
                    "Authorization" = "Bearer %teamcity.stage.auth_token%"
                }
                ${'$'}AuthParameters = @{
                    Method      = "GET"
                    Uri         = "%teamcity.stage.server%%teamcity.auth_endpoint%"
                    Headers     = ${'$'}AuthHeader
                }
                ${'$'}csrfToken = Invoke-RestMethod @AuthParameters
                
                ${'$'}Header = @{
                    "Authorization" = "Bearer %teamcity.stage.auth_token%"
                    "Content-Type" = "application/json"
                    "X-TC-CSRF-Token" = ${'$'}csrfToken
                }
                
                ${'$'}Body = '{
                    "buildType": {
                        "id": "%teamcity.stage.build_config_id%"
                    },
                    "properties": {
                        "property": [{
                                "name": "env.RELEASE_NUMBER",
                                "value": "v1.0.%build.counter%"
                            }
                        ]
                    }
                }'
                
                ${'$'}Parameters = @{
                    Method      = "POST"
                    Uri         = "%teamcity.stage.server%%teamcity.build_queue_endpoint%"
                    Headers     = ${'$'}Header
                    Body        = ${'$'}Body
                }
                Invoke-RestMethod @Parameters
            """.trimIndent()
        }
        script {
            name = "Call rest api powershell (1)"
            enabled = false
            scriptContent = """
                ${'$'}AuthHeader = @{
                    "Authorization" = "Bearer %teamcity.stage.auth_token%"
                }
                ${'$'}AuthParameters = @{
                    Method      = "GET"
                    Uri         = "%teamcity.stage.server%%teamcity.auth_endpoint%"
                    Headers     = ${'$'}AuthHeader
                }
                ${'$'}csrfToken = Invoke-RestMethod @AuthParameters
                
                ${'$'}Header = @{
                    "Authorization" = "Bearer %teamcity.stage.auth_token%"
                    "Content-Type" = "application/json"
                    "X-TC-CSRF-Token" = ${'$'}csrfToken
                }
                
                ${'$'}Body = '{
                    "buildType": {
                        "id": "%teamcity.stage.build_config_id%"
                    },
                    "properties": {
                        "property": [{
                                "name": "env.RELEASE_NUMBER",
                                "value": "v1.0.%build.counter%"
                            }
                        ]
                    }
                }'
                
                ${'$'}Parameters = @{
                    Method      = "POST"
                    Uri         = "%teamcity.stage.server%%teamcity.build_queue_endpoint%"
                    Headers     = ${'$'}Header
                    Body        = ${'$'}Body
                }
                
                ${'$'}Body | Write-Output
                
                Invoke-RestMethod @Parameters
            """.trimIndent()
        }
        powerShell {
            name = "actual script"
            enabled = false
            scriptMode = script {
                content = """
                    ${'$'}AuthHeader = @{
                        "Authorization" = "Bearer %teamcity.stage.auth_token%"
                    }
                    ${'$'}AuthParameters = @{
                        Method      = "GET"
                        Uri         = "%teamcity.stage.server%%teamcity.auth_endpoint%"
                        Headers     = ${'$'}AuthHeader
                    }
                    ${'$'}csrfToken = Invoke-RestMethod @AuthParameters
                    
                    ${'$'}Header = @{
                        "Authorization" = "Bearer %teamcity.stage.auth_token%"
                        "Content-Type" = "application/json"
                        "X-TC-CSRF-Token" = ${'$'}csrfToken
                    }
                    
                    ${'$'}Body = '{
                        "buildType": {
                            "id": "%teamcity.stage.build_config_id%"
                        },
                        "properties": {
                            "property": [{
                                    "name": "env.RELEASE_NUMBER",
                                    "value": "v1.0.%build.counter%"
                                }
                            ]
                        }
                    }'
                    
                    ${'$'}Parameters = @{
                        Method      = "POST"
                        Uri         = "%teamcity.stage.server%%teamcity.build_queue_endpoint%"
                        Headers     = ${'$'}Header
                        Body        = ${'$'}Body
                    }
                    
                    Invoke-RestMethod @Parameters
                """.trimIndent()
            }
        }
        script {
            name = "Notify Deploy Track"
            enabled = false
            scriptContent = """
                #!/usr/bin/env bash
                set -euo pipefail
                curl -fsS -H "Content-Type: application/json" -d '{
                    "kotahi_id":"%component.uuid%",
                    "environment":"%component.deployment_environment%",
                    "commit":"%build.vcs.number%",
                    "status": "in_progress",
                    "deployer": "%deployment.user%",
                    "url": "%teamcity.serverUrl%/viewLog.html?buildId=%teamcity.build.id%&buildTypeId=%system.teamcity.buildType.id%"
                }' -H "Authorization: Bearer %deploytrack.token%" -XPOST %deploytrack.url%
            """.trimIndent()
        }
        script {
            name = "Set deployer"
            enabled = false
            scriptContent = """
                #!/bin/bash
                
                if [[ "Unknown" == "Unknown" ]]
                then
                    # set the deployment user for use if the deploy is triggered by git
                    if [[ "%teamcity.build.triggeredBy.username%" == "n/a" ]]
                    then
                        gituser=${'$'}(git log -1 --pretty=format:'%an')
                        echo "Deployment triggered by  ${'$'}{gituser}"
                        echo "##teamcity[setParameter name='deployment.user' value='${'$'}{gituser}']"
                    else
                        echo "Deployment triggered by  %teamcity.build.triggeredBy.username%"
                        echo "##teamcity[setParameter name='deployment.user' value='%teamcity.build.triggeredBy.username%']"
                    fi
                else
                    echo "Deployment triggered by  Unknown"
                    echo "##teamcity[setParameter name='deployment.user' value='Unknown']"
                fi
            """.trimIndent()
        }
        script {
            name = "Set deployer 2"
            enabled = false
            executionMode = BuildStep.ExecutionMode.ALWAYS
            scriptContent = """
                #!/bin/bash
                
                # set the deployment user for use if the deploy is triggered by git
                if [[ "%teamcity.build.triggeredBy.username%" == "n/a" ]]
                then
                	gituser=${'$'}(git log -1 --pretty=format:'%an')
                	echo "Deployment triggered by  ${'$'}{gituser}"
                	echo "##teamcity[setParameter name='deployment.user' value='${'$'}{gituser}']"
                else
                	echo "Deployment triggered by  %teamcity.build.triggeredBy.username%"
                	echo "##teamcity[setParameter name='deployment.user' value='%teamcity.build.triggeredBy.username%']"
                fi
                if [[ -z "%teamcity.build.triggeredBy.username%" ]]
                then
                    echo "Deployment triggered by  Unknown"
                    echo "##teamcity[setParameter name='deployment.user' value='Unknown']"
                fi
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

object PostmanTests : BuildType({
    name = "Postman Tests"

    params {
        password("env.Delete_Auth_Header", "credentialsJSON:be2340ee-fa95-4882-8491-4013dfaa46e1")
        password("env.SecretTest", "credentialsJSON:f0109709-9459-40c8-ab2f-122d462c5989", label = "pass")
    }

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        script {
            name = "npm install"
            workingDir = "tests"
            scriptContent = "npm install"
        }
        script {
            name = "run tests"
            workingDir = "tests"
            scriptContent = """
                npm run runtest --Delete_Auth_Header=%env.Delete_Auth_Header%
                echo "Tests completed."
            """.trimIndent()
        }
        nodeJS {
            enabled = false
            workingDir = "tests"
            shellScript = "npm install"
            dockerPull = true
        }
    }
})

object PostmanTestsBoth : BuildType({
    name = "Postman Tests BOTH"

    params {
        password("env.Delete_Auth_Header", "credentialsJSON:be2340ee-fa95-4882-8491-4013dfaa46e1")
        password("env.SecretTest", "credentialsJSON:f0109709-9459-40c8-ab2f-122d462c5989", label = "pass")
    }

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        script {
            name = "npm install"
            workingDir = "tests"
            scriptContent = "npm install"
        }
        powerShell {
            name = "run tests"
            platform = PowerShellStep.Platform.x86
            edition = PowerShellStep.Edition.Desktop
            scriptMode = script {
                content = """
                    cd tests
                    npm run runtest-both --Delete_Auth_Header=%env.Delete_Auth_Header%
                """.trimIndent()
            }
            param("plugin.docker.imagePlatform", "linux")
            param("plugin.docker.imageId", "node:16")
        }
        nodeJS {
            enabled = false
            workingDir = "tests"
            shellScript = "npm install"
            dockerPull = true
        }
    }
})

object PostmanTestsPowershell : BuildType({
    name = "Postman Tests Powershell"

    params {
        password("env.Delete_Auth_Header", "credentialsJSON:be2340ee-fa95-4882-8491-4013dfaa46e1")
        password("env.SecretTest", "credentialsJSON:f0109709-9459-40c8-ab2f-122d462c5989", label = "pass")
    }

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        script {
            name = "npm install"
            workingDir = "tests"
            scriptContent = "npm install"
        }
        powerShell {
            name = "run tests"
            platform = PowerShellStep.Platform.x86
            edition = PowerShellStep.Edition.Desktop
            scriptMode = script {
                content = """
                    cd tests
                    npm run runtest-win --Delete_Auth_Header=%env.Delete_Auth_Header%
                """.trimIndent()
            }
        }
        nodeJS {
            enabled = false
            workingDir = "tests"
            shellScript = "npm install"
            dockerPull = true
        }
    }
})


object TestSubProject : Project({
    name = "TestSubProject"

    buildType(TestSubProject_BuildMaster)
})

object TestSubProject_BuildMaster : BuildType({
    name = "Build (master)"
})
