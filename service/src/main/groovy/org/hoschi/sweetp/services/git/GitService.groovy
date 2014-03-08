package org.hoschi.sweetp.services.git

import groovy.json.JsonBuilder
import groovy.util.logging.Log4j
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.LogCommand
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.hoschi.sweetp.services.base.ServiceParameter
import org.hoschi.sweetp.services.base.RouterMethod
import org.hoschi.sweetp.services.base.IHookManager
import org.hoschi.sweetp.services.base.IRouter

/**
 * Service for the version control system git.
 *
 * @author Stefan Gojan
 */
@Log4j
class GitService {
	IRouter router
	String config
	EasyFileRepositoryBuilder repositoryBuilder
	GitSimpleWrapper wrapper

	/**
	 * Description which properties a commit object has.
	 */
	static final Map COMMITMSG = [
			name: 'full commit hash',
			fullMessage: 'full commit message',
			shortMessage: 'short commit message'
	]

	/**
	 * Build config and dependencies.
	 */
	GitService() {
		JsonBuilder json = new JsonBuilder([
				['/scm/branch/name': [
						method: 'getBranchName',
						route: [
								method: RouterMethod.CONFIG_EXISTS,
								property: 'git'
						],
						params: [
								config: ServiceParameter.PROJECT_CONFIG
						],
						description: description('Branch name of HEAD.'),
						returns: 'branch name as string'
				]],
				['/scm/commit': [
						method: 'commit',
						route: [
								method: RouterMethod.CONFIG_EXISTS,
								property: 'git'
						],
						params: [
								config: ServiceParameter.PROJECT_CONFIG,
								message: ServiceParameter.ONE,
								switches: ServiceParameter.LIST
						],
						description: description('git commit -m message'),
						returns: 'git commit command output as string'
				]],
				['/scm/merge': [
						method: 'merge',
						route: [
								method: RouterMethod.CONFIG_EXISTS,
								property: 'git'
						],
						params: [
								config: ServiceParameter.PROJECT_CONFIG,
								remote: ServiceParameter.ONE,
								switches: ServiceParameter.LIST
						],
						hooks: [
								pub: ['/scm/preMerge']
						],
						description: description('git merge other'),
						returns: 'git merge command output as string'
				]],
				['/scm/log': [
						method: 'log',
						route: [
								method: RouterMethod.CONFIG_EXISTS,
								property: 'git'
						],
						params: [
								config: ServiceParameter.PROJECT_CONFIG,
								since: ServiceParameter.ONE,
								until: ServiceParameter.ONE,
                                limit: ServiceParameter.ONE
						],
						description: description("List commits. You can provide optional 'since' and 'until' points in the scm graph to give your search a border. If one of both is set, but not the other one it defaults to 'HEAD'."),
						returns: [list: COMMITMSG]
				]],
				['/scm/commit/by/ref': [
						method: 'getCommitByRef',
						route: [
								method: RouterMethod.CONFIG_EXISTS,
								property: 'git'
						],
						params: [
								config: ServiceParameter.PROJECT_CONFIG,
								name: ServiceParameter.ONE
						],
						description: description("Find a commit by it's ref, like HEAD, master, etc."),
						returns: COMMITMSG
				]]
		])
		config = json.toString()
		repositoryBuilder = new EasyFileRepositoryBuilder()
		wrapper = new GitSimpleWrapper()
	}

	protected Map description(String summary) {
		[
				summary: summary,
				config: 'needs the location of the ".git" dir of your repository. This should be simple ".git" in the current directory.',
				example: '''
<pre>
{
    "name": "testgit",
        "git": {
                "dir":".git"
        }
}
</pre>
'''
		]
	}

	/**
	 * Branch name of HEAD.
	 *
	 * @param params contain the service config
	 * @return branch name
	 */
	String getBranchName(Map params) {
		assert params.config.git.dir

		log.info 'test'

		Repository repo = repositoryBuilder.buildRepo(params.config.dir,
				params.config.git.dir)
		repo.branch
	}

	/**
	 * git merge
	 *
	 * @param params contain the service config and remote ref
	 * @return output of 'git merge'
	 */
	String merge(Map params) {
		assert params.config.dir
		assert params.config.git.dir
		assert params.remote
		assert router.hooks

		IHookManager hooks = router.hooks
		StringWriter ret = new StringWriter()

		// execute pre hook
		Map pre = hooks.callHook('/scm/preMerge', [until: params.remote],
						params.config)
				if (!pre || !pre.allOk) {
					ret.println 'hook "preMerge" returned with error:'
					ret.println pre.error
					return ret.toString()
				}

		// set executable
		List command = ['git', 'merge']
		command.addAll(params.switches)
		command << params.remote
		log.debug "command is $command"
		ProcessBuilder builder = new ProcessBuilder(command)

		// set working dir
		File gitDir = new File(params.config.dir + '/'
				+ params.config.git.dir as String)
		builder.directory gitDir.canonicalFile.parentFile
		log.debug "working dir is ${builder.directory()}"

		// start process and tie it to this thread
		Process process = builder.start()
		StringBuffer out = new StringBuffer()
		StringBuffer err = new StringBuffer()
		process.waitForProcessOutput(out, err)
		ret << "$out\n$err"
		ret.toString()
	}

	/**
	 * git commit -m message
	 *
	 * @param params contain the service config and commit message
	 * @return output of 'git commit'
	 */
	String commit(Map params) {
		assert params.config.dir
		assert params.config.git.dir
		assert params.message

		// this uses the cli from git because jgit make empty commits without
		// any possibility to prevent this. Empty commits can stop rebasing.

		// set executable
		List command = ['git', 'commit']
		command.addAll(params.switches)
		command << '-m'
		command << params.message
		log.debug "command is $command"
		ProcessBuilder builder = new ProcessBuilder(command)

		// set working dir
		File gitDir = new File(params.config.dir + '/'
				+ params.config.git.dir as String)
		builder.directory gitDir.canonicalFile.parentFile
		log.debug "working dir is ${builder.directory()}"

		// start process and tie it to this thread
		Process process = builder.start()
		StringBuffer out = new StringBuffer()
		StringBuffer err = new StringBuffer()
		process.waitForProcessOutput(out, err)
		"$out\n$err"
	}

	/**
	 * Find a commit by it's name, like HEAD, master, etc.
	 *
	 * @param params contain config and name of commit
	 * @return commit
	 */
	Object getCommitByRef(Map params) {
		assert params.name
		assert params.config.dir
		assert params.config.git.dir

		Repository repo = repositoryBuilder.buildRepo(params.config.dir,
				params.config.git.dir)
		wrapper.wrap(getCommitByRef(params.name, repo))
	}

	/**
	 * Helper method to parse a ref and find it's commit.
	 *
	 * @param name of ref
	 * @param repo which to search in
	 * @return null or a commit object
	 */
	protected RevCommit getCommitByRef(String name, Repository repo) {
		def ref = repo.getRef(name)
		assert ref, "no ref found with name $name"

		RevWalk walk = new RevWalk(repo)
		RevCommit commit = walk.parseCommit(ref.objectId)
		assert commit, "no commit found with name $ref.name"
		walk.dispose()

		commit
	}

	/**
	 * List commits. You can provide optional 'since' and 'until' points in
	 * the scm graph to give your search a border. If one of both is set, but
	 * not the other one it defaults to 'HEAD'.
	 *
	 * @param params provides git related stuff and the optional 'until'/'since'
	 * refs.
	 * @return the search result
	 */
	Object log(Map params) {
        Integer limit
		assert params.config.dir
		assert params.config.git.dir

		Repository repo = repositoryBuilder.buildRepo(params.config.dir,
				params.config.git.dir)

		// From git log manual:
		// Show only commits between the named two commits. When either since or
		// until is omitted, it defaults to HEAD.
		RevCommit since = null
		RevCommit until = null
		if (params.since && params.until) {
			since = getCommitByRef(params.since, repo)
			until = getCommitByRef(params.until, repo)
		} else if (params.since && !params.until) {
			since = getCommitByRef(params.since, repo)
			until = getCommitByRef('HEAD', repo)
		} else if (!params.since && params.until) {
			since = getCommitByRef('HEAD', repo)
			until = getCommitByRef(params.until, repo)
		}

        if (params.limit) {
            limit = params.limit.toInteger()
        } else {
            limit = 0;
        }

		log.debug "since: $since, until: $until, limit: $limit"

		Git git = new Git(repo)
		LogCommand cmd = git.log()
		if (since && until) {
			cmd.addRange(since.id, until.id)
		}

        if (limit) {
            cmd.setMaxCount(limit);
        }
		wrapper.wrap(cmd.call())
	}
}
