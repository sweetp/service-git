package org.hoschi.sweetp.services.git

import org.eclipse.jgit.revwalk.RevCommit
import org.hoschi.sweetp.services.base.SimpleWrapper

/**
 * Wrapper to simplify git objects.
 *
 * @author Stefan Gojan
 */
class GitSimpleWrapper extends SimpleWrapper {

	/**
	 * Transform a commit into name, full and short message.
	 *
	 * @param commit to transform
	 * @return simple Map with three keys
	 */

	Map wrap(RevCommit commit) {
		if (!commit) return [:]
		[
				name: commit.name(),
				fullMessage: commit.fullMessage,
				shortMessage: commit.shortMessage
		]
	}
}
