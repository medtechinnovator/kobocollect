package org.odk.collect.android.mainmenu

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.Gravity
import android.graphics.Paint
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.odk.collect.android.activities.DeleteFormsActivity
import org.odk.collect.android.activities.FormDownloadListActivity
import org.odk.collect.android.activities.InstanceChooserList
import org.odk.collect.android.application.MapboxClassInstanceCreator
import org.odk.collect.android.databinding.MainMenuBinding
import org.odk.collect.android.formentry.FormOpeningMode
import org.odk.collect.android.formlists.blankformlist.BlankFormListActivity
import org.odk.collect.android.formmanagement.FormFillingIntentFactory
import org.odk.collect.android.instancemanagement.send.InstanceUploaderListActivity
import org.odk.collect.android.projects.ProjectSettingsDialog
import org.odk.collect.android.utilities.ActionRegister
import org.odk.collect.androidshared.data.consume
import org.odk.collect.androidshared.ui.DialogFragmentUtils
import org.odk.collect.androidshared.ui.SnackbarUtils
import org.odk.collect.androidshared.ui.multiclicksafe.MultiClickGuard
import org.odk.collect.metadata.PropertyManager
import org.odk.collect.settings.SettingsProvider
import org.odk.collect.settings.keys.ProjectKeys
import org.odk.collect.strings.R.string
import org.odk.collect.webpage.WebViewActivity
import org.odk.collect.androidshared.ui.ToastUtils

class MainMenuFragment(
    private val viewModelFactory: ViewModelProvider.Factory,
    private val settingsProvider: SettingsProvider,
    private val propertyManager: PropertyManager
) : Fragment() {

    companion object {
        /** Hardcoded 4-digit PIN required to update username from the homepage. */
        private const val USERNAME_EDIT_PIN = "9876"
    }

    private lateinit var mainMenuViewModel: MainMenuViewModel
    private lateinit var currentProjectViewModel: CurrentProjectViewModel
    private lateinit var permissionsViewModel: RequestPermissionsViewModel

    private val formEntryFlowLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uri = result.data?.data
            mainMenuViewModel.setSavedForm(uri)
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val viewModelProvider = ViewModelProvider(requireActivity(), viewModelFactory)
        mainMenuViewModel = viewModelProvider[MainMenuViewModel::class.java]
        currentProjectViewModel = viewModelProvider[CurrentProjectViewModel::class.java]
        permissionsViewModel = viewModelProvider[RequestPermissionsViewModel::class.java]

        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return MainMenuBinding.inflate(inflater, container, false).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        currentProjectViewModel.currentProject.observe(viewLifecycleOwner) { project ->
            if (project != null) {
                requireActivity().invalidateOptionsMenu()
            }
        }

        val binding = MainMenuBinding.bind(view)
        initToolbar(binding)
        initMapbox()
        initButtons(binding)
        initUserIdentity(binding)

        if (permissionsViewModel.shouldAskForPermissions()) {
            DialogFragmentUtils.showIfNotShowing(
                PermissionsDialogFragment::class.java,
                this.parentFragmentManager
            )
        }

        mainMenuViewModel.savedForm.consume(viewLifecycleOwner) { value ->
            SnackbarUtils.showSnackbar(
                requireView(),
                getString(value.message),
                SnackbarUtils.DURATION_LONG,
                action = value.action?.let { action ->
                    SnackbarUtils.Action(getString(action)) {
                        formEntryFlowLauncher.launch(
                            FormFillingIntentFactory.editDraftFormIntent(
                                requireContext(),
                                value.uri
                            )
                        )
                    }
                },
                displayDismissButton = true
            )
        }

        currentProjectViewModel.currentProject.observe(viewLifecycleOwner) {
            if (it?.isOldGoogleDriveProject == true) {
                binding.googleDriveDeprecationBanner.root.visibility = View.VISIBLE
                binding.googleDriveDeprecationBanner.learnMoreButton.setOnClickListener {
                    val intent = Intent(requireContext(), WebViewActivity::class.java)
                    intent.putExtra("url", "https://forum.getodk.org/t/40097")
                    startActivity(intent)
                }
            } else {
                binding.googleDriveDeprecationBanner.root.visibility = View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mainMenuViewModel.refreshInstances()

        val binding = MainMenuBinding.bind(requireView())
        setButtonsVisibility(binding)
        refreshUserIdentityDisplay(binding)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val projectsMenuItem = menu.findItem(org.odk.collect.android.R.id.projects)
        projectsMenuItem.actionView?.setOnClickListener { onOptionsItemSelected(projectsMenuItem) }
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(org.odk.collect.android.R.menu.main_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (!MultiClickGuard.allowClick(javaClass.name)) {
            return true
        }
        if (item.itemId == org.odk.collect.android.R.id.projects) {
            DialogFragmentUtils.showIfNotShowing(
                ProjectSettingsDialog::class.java,
                parentFragmentManager
            )
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun initToolbar(binding: MainMenuBinding) {
        val toolbar = binding.root.findViewById<Toolbar>(org.odk.collect.androidshared.R.id.toolbar)
        (requireActivity() as AppCompatActivity).apply {
            setSupportActionBar(toolbar)
            title = "" // Prevent activity label (e.g. app name) from showing in toolbar
        }
        toolbar.title = ""
        toolbar.setContentInsetStartWithNavigation(0)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(false)
        // Use custom ImageView instead of setLogo() so we can position it flush left (no internal Toolbar padding)
        val logoView = ImageView(requireContext()).apply {
            setImageResource(org.odk.collect.android.R.drawable.mti_logo)
            adjustViewBounds = true
            maxHeight = resources.getDimensionPixelSize(android.R.dimen.app_icon_size)
            contentDescription = getString(string.collect_app_name)
        }
        val logoParams = Toolbar.LayoutParams(
            Toolbar.LayoutParams.WRAP_CONTENT,
            Toolbar.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            marginStart = 0
            leftMargin = 0
        }
        toolbar.addView(logoView, 0, logoParams)
    }

    private fun initUserIdentity(binding: MainMenuBinding) {
        val valueView = binding.userIdentitySection.userIdentityValue
        valueView.paintFlags = valueView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        refreshUserIdentityDisplay(binding)

        valueView.setOnClickListener {
            showPinDialog(valueView)
        }
    }

    private fun showPinDialog(valueView: TextView) {
        val pinInput = EditText(requireContext()).apply {
            hint = getString(org.odk.collect.strings.R.string.main_menu_pin_hint)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setSingleLine(true)
            filters = arrayOf(InputFilter.LengthFilter(4))
            setPadding(
                resources.getDimensionPixelSize(org.odk.collect.androidshared.R.dimen.margin_standard),
                resources.getDimensionPixelSize(org.odk.collect.androidshared.R.dimen.margin_small),
                resources.getDimensionPixelSize(org.odk.collect.androidshared.R.dimen.margin_standard),
                resources.getDimensionPixelSize(org.odk.collect.androidshared.R.dimen.margin_small)
            )
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(org.odk.collect.strings.R.string.main_menu_pin_title)
            .setView(pinInput)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val entered = pinInput.text.toString()
                if (entered == USERNAME_EDIT_PIN) {
                    showEditUsernameDialog(valueView)
                } else {
                    ToastUtils.showShortToast(org.odk.collect.strings.R.string.main_menu_pin_incorrect)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun refreshUserIdentityDisplay(binding: MainMenuBinding) {
        val valueView = binding.userIdentitySection.userIdentityValue
        val username = settingsProvider.getUnprotectedSettings().getString(ProjectKeys.KEY_METADATA_USERNAME)
            ?: ""
        val fallback = settingsProvider.getUnprotectedSettings().getString(ProjectKeys.KEY_USERNAME)
        val display = when {
            username.isNotBlank() -> username
            !fallback.isNullOrBlank() -> fallback
            else -> getString(org.odk.collect.strings.R.string.main_menu_user_not_set)
        }
        valueView.text = display
    }

    private fun showEditUsernameDialog(valueView: TextView) {
        val current = settingsProvider.getUnprotectedSettings().getString(ProjectKeys.KEY_METADATA_USERNAME)
            ?: settingsProvider.getUnprotectedSettings().getString(ProjectKeys.KEY_USERNAME)
            ?: ""
        val input = EditText(requireContext()).apply {
            setText(current)
            hint = getString(string.username)
            setSingleLine(true)
            setPadding(
                resources.getDimensionPixelSize(org.odk.collect.androidshared.R.dimen.margin_standard),
                resources.getDimensionPixelSize(org.odk.collect.androidshared.R.dimen.margin_small),
                resources.getDimensionPixelSize(org.odk.collect.androidshared.R.dimen.margin_standard),
                resources.getDimensionPixelSize(org.odk.collect.androidshared.R.dimen.margin_small)
            )
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(org.odk.collect.strings.R.string.main_menu_edit_username_title)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newValue = input.text.toString().trim()
                settingsProvider.getUnprotectedSettings().save(ProjectKeys.KEY_METADATA_USERNAME, newValue)
                propertyManager.reload()
                valueView.text = if (newValue.isNotBlank()) newValue else getString(org.odk.collect.strings.R.string.main_menu_user_not_set)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun initMapbox() {
        if (MapboxClassInstanceCreator.isMapboxAvailable()) {
            childFragmentManager
                .beginTransaction()
                .add(
                    org.odk.collect.android.R.id.map_box_initialization_fragment,
                    MapboxClassInstanceCreator.createMapBoxInitializationFragment()!!
                )
                .commit()
        }
    }

    private fun initButtons(binding: MainMenuBinding) {
        binding.enterData.setOnClickListener {
            ActionRegister.actionDetected()

            formEntryFlowLauncher.launch(
                Intent(requireActivity(), BlankFormListActivity::class.java)
            )
        }

        binding.reviewData.setOnClickListener {
            formEntryFlowLauncher.launch(
                Intent(requireActivity(), InstanceChooserList::class.java).apply {
                    putExtra(
                        FormOpeningMode.FORM_MODE_KEY,
                        FormOpeningMode.EDIT_SAVED
                    )
                }
            )
        }

        binding.sendData.setOnClickListener {
            formEntryFlowLauncher.launch(
                Intent(
                    requireActivity(),
                    InstanceUploaderListActivity::class.java
                )
            )
        }

        binding.viewSentForms.setOnClickListener {
            startActivity(
                Intent(requireActivity(), InstanceChooserList::class.java).apply {
                    putExtra(
                        FormOpeningMode.FORM_MODE_KEY,
                        FormOpeningMode.VIEW_SENT
                    )
                }
            )
        }

        binding.getForms.setOnClickListener {
            val intent = Intent(requireContext(), FormDownloadListActivity::class.java)
            startActivity(intent)
        }

        binding.manageForms.setOnClickListener {
            startActivity(Intent(requireContext(), DeleteFormsActivity::class.java))
        }

        mainMenuViewModel.sendableInstancesCount.observe(viewLifecycleOwner) { finalized: Int ->
            binding.sendData.setNumberOfForms(finalized)
        }
        mainMenuViewModel.editableInstancesCount.observe(viewLifecycleOwner) { unsent: Int ->
            binding.reviewData.setNumberOfForms(unsent)
        }
        mainMenuViewModel.sentInstancesCount.observe(viewLifecycleOwner) { sent: Int ->
            binding.viewSentForms.setNumberOfForms(sent)
        }
    }

    private fun setButtonsVisibility(binding: MainMenuBinding) {
        binding.reviewData.visibility =
            if (mainMenuViewModel.shouldEditSavedFormButtonBeVisible()) View.VISIBLE else View.GONE
        binding.sendData.visibility =
            if (mainMenuViewModel.shouldSendFinalizedFormButtonBeVisible()) View.VISIBLE else View.GONE
        binding.viewSentForms.visibility =
            if (mainMenuViewModel.shouldViewSentFormButtonBeVisible()) View.VISIBLE else View.GONE
        binding.getForms.visibility =
            if (mainMenuViewModel.shouldGetBlankFormButtonBeVisible()) View.VISIBLE else View.GONE
        binding.manageForms.visibility =
            if (mainMenuViewModel.shouldDeleteSavedFormButtonBeVisible()) View.VISIBLE else View.GONE
    }
}
