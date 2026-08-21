/*
 * Copyright (C) 2026 Kainy
 *
 * This file is part of AbyssFall.
 *
 * AbyssFall is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AbyssFall is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AbyssFall.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.abyssfall.agreement;

/**
 * The wording of the testing agreement, in both languages, and the one place it is written down.
 *
 * <h2>Why the text is hard-coded rather than translated</h2>
 *
 * <p>This runs from the {@code preLaunch} entrypoint, several seconds before Minecraft itself
 * starts. There is no resource manager, no language file loaded, and no {@code Component} to
 * translate — so the mod's {@code lang} files cannot reach this text and both languages have to be
 * present in the source. Every message is therefore given twice, Chinese first, matching how the
 * mod's other player-facing strings are ordered.
 *
 * <p>Kept apart from the classes that display it so that the console notice and the dialog cannot
 * drift into saying different things.
 */
final class AgreementText {
	/**
	 * Where the source lives, and what the dialog's button copies.
	 */
	static final String REPOSITORY_URL = "https://github.com/Kainy030/AbyssFall-Fabric";

	/**
	 * Where reports and questions are meant to go.
	 */
	static final String ISSUES_URL = "https://github.com/Kainy030/AbyssFall-Fabric/issues";

	/**
	 * The agreement itself: what a tester is being asked to accept.
	 */
	static final String NOTICE_ZH =
			"AbyssFall 目前处于早期开发测试阶段。"
			+ "此版本并非正式发布版本，可能存在严重 Bug、未完成内容以及其他预期之外的问题。"
			+ "在本阶段参与测试期间，请勿以任何方式传播、分发或向第三方提供此版本的 AbyssFall。"
			+ "如果发现 Bug、异常行为或其他问题，请通过作者或 AbyssFall GitHub 仓库提交反馈。";

	static final String NOTICE_EN =
			"AbyssFall is currently in an early development testing stage."
			+ "This is not a public release and may contain severe bugs, unfinished content, and other unexpected issues."
			+ "While participating in this testing stage, please do not share, redistribute, or otherwise provide this version of AbyssFall to third parties."
			+ "If you encounter a bug, unexpected behavior, or any other issue, please report it to the author or through the AbyssFall GitHub repository.";

	/**
	 * What the dialog asks for, and what a client tester has to type.
	 */
	static final String PROMPT_ZH = "如果您已阅读并接受以上测试约定，请在下方输入 accept 以继续启动。";

	static final String PROMPT_EN = "If you have read and accept the testing agreement above, type accept below to continue launching.";

	/**
	 * Why a dedicated server is told rather than asked.
	 */
	static final String SERVER_ZH =
			"检测到当前环境为 Dedicated Server。服务器环境无法显示交互式测试确认窗口，因此本次仅记录测试声明，不会阻止服务器启动。"
			+ "将 AbyssFall 部署于此环境即表示您已看到并知悉上述测试约定。";

	static final String SERVER_EN =
			"A dedicated server environment has been detected. Because a server cannot display the interactive testing agreement, this launch will only record the notice and will not be blocked."
			+ "Deploying AbyssFall in this environment indicates that you have been shown and informed of the testing agreement above.";

	/**
	 * What is logged when a client tester accepts.
	 */
	static final String ACCEPTED_ZH = "已接受 AbyssFall 测试约定，继续加载。";

	static final String ACCEPTED_EN = "AbyssFall testing agreement accepted; continuing launch.";

	/**
	 * What is logged and shown when a client tester does not.
	 */
	static final String DECLINED_ZH =
			"未接受 AbyssFall 测试约定，AbyssFall 将拒绝启动。若要参与测试，请重新启动游戏并输入 accept。";

	static final String DECLINED_EN =
			"The AbyssFall testing agreement was not accepted. AbyssFall will refuse to start."
			+ "Restart the game and type accept to participate in testing.";

	/**
	 * What is logged when there is no display to ask on, and no answer can be obtained.
	 */
	static final String HEADLESS_ZH =
			"当前客户端环境没有可用的图形界面，无法显示交互式测试约定。已切换至非交互式通知模式，启动将继续。";

	static final String HEADLESS_EN =
			"No graphical interface is available in the current client environment, so the interactive testing agreement cannot be displayed."
			+ "The launch has been switched to non-interactive notice mode and will continue.";

	private AgreementText() {
	}

	/**
	 * Both languages of one message, one per line, for the log.
	 */
	static String bilingual(String chinese, String english) {
		return chinese + System.lineSeparator() + english;
	}
}
