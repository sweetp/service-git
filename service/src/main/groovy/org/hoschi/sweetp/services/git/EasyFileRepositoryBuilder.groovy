package org.hoschi.sweetp.services.git

import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

/**
 * Easy RepoBuilder for JGit. With this Class it is easier to mock repository
 * creation in main service class.
 * @see GitService
 * @author Stefan Gojan
 */
class EasyFileRepositoryBuilder {

	/**
	 * Builds a repository at the directory "base/git". Additional it checks whether
	 * "base/git" exit or not.
	 *
	 * @param base directory (canonical)
	 * @param git directory (relative from base dir)
	 * @return git repository for given dir
	 */
	Repository buildRepo(String base, String git) {
		String dir = base + File.separator + git
		File gitDir = new File(dir)
		if (!gitDir.exists()) {
			throw new FileNotFoundException('File not found' + dir)
		}

		FileRepositoryBuilder builder = new FileRepositoryBuilder()
		builder.setGitDir(gitDir).readEnvironment().findGitDir().build()
	}
}
