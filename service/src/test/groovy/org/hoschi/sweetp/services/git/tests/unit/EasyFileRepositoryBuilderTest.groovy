package org.hoschi.sweetp.services.git.org.hoschi.sweetp.services.git.tests.unit

import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gmock.WithGMock
import org.hoschi.sweetp.services.git.EasyFileRepositoryBuilder
import org.junit.Before
import org.junit.Test

/**
 * @author Stefan Gojan
 */
@WithGMock
class EasyFileRepositoryBuilderTest {
	EasyFileRepositoryBuilder builder

	@Before
	void setUp() {
		builder = new EasyFileRepositoryBuilder()
	}

	@Test(expected = FileNotFoundException)
	void testBuildRepositroryWithNotExistingDir() {
		String dir = '/foo/.git'
		File gitDir = mock(File, constructor(dir))
		gitDir.exists().returns(false)

		play {
			assert builder.buildRepo('/foo', '.git')
		}
	}

	@Test
	void testBuildRepositoryWithString() {
		Repository repo = mock(Repository)
		String dir = '/foo/.git'
		File gitDir = mock(File, constructor(dir))
		gitDir.exists().returns(true)

		FileRepositoryBuilder fileRepositoryBuilder = mock(FileRepositoryBuilder, constructor())
		fileRepositoryBuilder.setGitDir(gitDir).returns(fileRepositoryBuilder)
		fileRepositoryBuilder.readEnvironment().returns(fileRepositoryBuilder)
		fileRepositoryBuilder.findGitDir().returns(fileRepositoryBuilder)
		fileRepositoryBuilder.build().returns(repo)

		play {
			assert builder.buildRepo('/foo', '.git') instanceof Repository
		}
	}
}
