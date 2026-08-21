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

import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The testing agreement that runs before anything else does.
 *
 * <h2>When this runs</h2>
 *
 * <p>Registered as a {@code preLaunch} entrypoint, which Fabric Loader invokes from
 * {@code Knot#init} after Mixin bootstrapping but before the game is launched — several seconds
 * ahead of the {@code main} and {@code client} entrypoints. Nothing of Minecraft exists yet, and
 * neither does the mod's own configuration, so this class can rely on neither.
 *
 * <h2>What it is for</h2>
 *
 * <p>To make sure nobody runs a pre-release build without being told that it is one, and to ask
 * testers not to pass it around while it is. It is a piece of communication rather than a
 * protection measure: the mod is GPL and open source, the check is trivially removed, and that is
 * fine. What it buys is that no tester can honestly say they were not asked.
 *
 * <h2>Why it is asked every launch</h2>
 *
 * <p>No record of having accepted is kept, deliberately. A remembered answer would turn the
 * agreement into a one-off click that the tester stops reading; asking each time keeps the notice
 * in front of the person who most needs to see it — the one about to record a video of an
 * unfinished mod. The development environment is exempt so that {@code runClient} stays usable.
 *
 * <h2>Why it has its own logger</h2>
 *
 * <p>{@link PreLaunchEntrypoint}'s own javadoc warns against touching anything that would run a
 * mod's static initialisers this early. Referring to {@code AbyssFall.LOGGER} would initialise the
 * mod's main class before the game exists, so this class stands alone with a logger of its own.
 */
public final class TestAgreement implements PreLaunchEntrypoint {
	private static final Logger LOGGER = LoggerFactory.getLogger("AbyssFall");

	/**
	 * What the tester has to type. Compared case-insensitively after trimming.
	 */
	private static final String PASSPHRASE = "accept";

	@Override
	public void onPreLaunch() {
		// A developer running their own build has already agreed with themselves.
		if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
			return;
		}

		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			askClient();
		} else {
			tellServer();
		}
	}

	/**
	 * Asks on screen, and refuses to continue unless the answer is the passphrase.
	 *
	 * <p>Accepting returns normally and the game carries on loading. Anything else throws, and
	 * Fabric Loader turns that into a {@code FormattedException}: the reason is written to the
	 * log at ERROR, an error window is shown, and the process exits without the game ever
	 * starting. Throwing is the documented way to stop a launch from here — there is no
	 * mechanism for a single mod to bow out and let the game continue without it, and pretending
	 * otherwise would leave a half-loaded mod behind.
	 */
	private static void askClient() {
		// A client without a display cannot be asked. Rather than crash on the dialog, say so
		// and fall through to the server's behaviour: told rather than asked.
		if (GraphicsEnvironment.isHeadless()) {
			LOGGER.warn(AgreementText.bilingual(
					AgreementText.HEADLESS_ZH, AgreementText.HEADLESS_EN));
			tellServer();
			return;
		}

		JTextField answerField = new JTextField(24);

		// Copies rather than opens. Launching a browser from here would put an unpredictable
		// external process in front of a game that has not started yet, and Desktop support is
		// not guaranteed on every platform; the clipboard is available wherever the dialog is.
		JButton copyLink = new JButton("复制仓库链接 / Copy repository link");
		copyLink.addActionListener(event -> {
			Toolkit.getDefaultToolkit().getSystemClipboard()
					.setContents(new StringSelection(AgreementText.REPOSITORY_URL), null);
			copyLink.setText("已复制 / Copied");
		});

		JPanel panel = new JPanel(new BorderLayout(0, 12));
		panel.add(new JLabel(dialogHtml()), BorderLayout.NORTH);
		panel.add(answerField, BorderLayout.CENTER);
		panel.add(copyLink, BorderLayout.SOUTH);

		int button = JOptionPane.showConfirmDialog(null, panel, "AbyssFall",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

		boolean accepted = button == JOptionPane.OK_OPTION
				&& PASSPHRASE.equalsIgnoreCase(answerField.getText().trim());

		if (accepted) {
			LOGGER.info(AgreementText.bilingual(
					AgreementText.ACCEPTED_ZH, AgreementText.ACCEPTED_EN));
			return;
		}

		LOGGER.error(AgreementText.bilingual(
				AgreementText.DECLINED_ZH, AgreementText.DECLINED_EN));

		throw new IllegalStateException(AgreementText.bilingual(
				AgreementText.DECLINED_ZH, AgreementText.DECLINED_EN));
	}

	/**
	 * Tells rather than asks, and never refuses to continue.
	 *
	 * <p>A dedicated server has no screen to put a dialog on and no operator sitting in front of
	 * it at start-up, so blocking the launch on an answer nobody can give would only mean the
	 * server never comes up. Deploying the mod here is taken as agreement, and the notice goes
	 * to the log where the administrator will see it.
	 */
	private static void tellServer() {
		LOGGER.warn(AgreementText.bilingual(
				AgreementText.NOTICE_ZH, AgreementText.NOTICE_EN));
		LOGGER.info(AgreementText.bilingual(
				AgreementText.SERVER_ZH, AgreementText.SERVER_EN));
		LOGGER.info("GitHub: {}", AgreementText.REPOSITORY_URL);
		LOGGER.info("Issues: {}", AgreementText.ISSUES_URL);
	}

	/**
	 * The notice and the prompt, as HTML so that Swing wraps it at a readable width.
	 *
	 * <p>A plain string would be laid out as one enormous line. The width is fixed rather than
	 * proportional because the dialog has no parent window to size itself against this early.
	 */
	private static String dialogHtml() {
		return "<html><body style='width: 460px'>"
				+ "<p>" + AgreementText.NOTICE_ZH + "</p>"
				+ "<p>" + AgreementText.NOTICE_EN + "</p>"
				+ "<p><b>" + AgreementText.PROMPT_ZH + "</b><br>"
				+ "<b>" + AgreementText.PROMPT_EN + "</b></p>"
				+ "</body></html>";
	}
}
