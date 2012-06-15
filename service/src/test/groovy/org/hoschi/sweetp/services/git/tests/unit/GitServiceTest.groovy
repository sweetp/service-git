package org.hoschi.sweetp.services.git.tests.unit

import org.eclipse.jgit.lib.Repository
import org.gmock.WithGMock
import org.hoschi.sweetp.services.git.EasyFileRepositoryBuilder
import org.hoschi.sweetp.services.git.GitService
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

/**
 * @author Stefan Gojan
 */
@WithGMock
class GitServiceTest {
	GitService service

	@Before
	void setUp() {
		service = new GitService()
	}

	@Test
	void getBranchName() {
		Map params = [
				config: [
						dir: '/home/hoschi/foo',
						git: [
								dir: '.git'
						]
				]]

		Repository repo = mock(Repository)
		EasyFileRepositoryBuilder repositoryBuilder = mock(EasyFileRepositoryBuilder)
		service.repositoryBuilder = repositoryBuilder

		repositoryBuilder.buildRepo(params.config.dir, params.config.git.dir).returns(repo)
		repo.branch.returns('feature/123')

		play {
			assert 'feature/123' == service.getBranchName(params)
		}
	}

	@Test
	@Ignore // ProcessBuilder can't be mocked
	@SuppressWarnings('EmptyMethod')
	void commit() {
	}
}
