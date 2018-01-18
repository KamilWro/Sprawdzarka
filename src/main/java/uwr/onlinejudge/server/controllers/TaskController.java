package uwr.onlinejudge.server.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uwr.onlinejudge.server.models.*;
import uwr.onlinejudge.server.models.form.SolutionForm;
import uwr.onlinejudge.server.models.form.TaskDescriptionForm;
import uwr.onlinejudge.server.models.form.TaskForm;
import uwr.onlinejudge.server.models.form.TaskListForm;
import uwr.onlinejudge.server.services.GroupService;
import uwr.onlinejudge.server.services.SolutionService;
import uwr.onlinejudge.server.services.TaskService;
import uwr.onlinejudge.server.services.UserService;
import uwr.onlinejudge.server.util.Languages;

import javax.validation.Valid;
import java.security.Principal;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;


@Controller
@RequestMapping
public class TaskController {
    private TaskService taskService;
    private UserService userService;
    private GroupService groupService;
    private SolutionService solutionService;

    @Autowired
    public TaskController(TaskService taskService, UserService userService, GroupService groupService, SolutionService solutionService) {
        this.taskService = taskService;
        this.userService = userService;
        this.groupService = groupService;
        this.solutionService = solutionService;
    }

    @RequestMapping(value = "/dodaj_liste/{id}", method = RequestMethod.GET)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String addList(@PathVariable("id") Long id, Model model) {
        Group group = groupService.getGroup(id);
        if (group == null)
            return "error_page";

        TaskListForm taskListForm = new TaskListForm();
        taskListForm.setGroup(group);

        model.addAttribute("taskList", taskListForm);
        return "forms/add_list";
    }

    @RequestMapping(value = "/dodaj_liste", method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String saveList(@ModelAttribute("taskList") @Valid TaskListForm taskListForm, BindingResult bindingResult, Principal principal, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "forms/add_list";
        }
        taskListForm.setUser(userService.findByEmail(principal.getName()));
        taskService.save(taskListForm);

        redirectAttributes.addFlashAttribute("alertMessage", "Lista została utworzona");
        return "redirect:/grupa/" + taskListForm.getGroup().getId();
    }

    @RequestMapping(value = "/dodaj_opis_zadania/{id}", method = RequestMethod.POST)
    @PreAuthorize("isFullyAuthenticated()")
    public String addTaskDescription(@PathVariable("id") Long id, @ModelAttribute("taskDescription") @Valid TaskDescriptionForm taskDescriptionForm, Principal principal, RedirectAttributes redirectAttributes) {

        taskDescriptionForm.setUser(userService.findByEmail(principal.getName()));
        taskService.save(taskDescriptionForm);

        redirectAttributes.addFlashAttribute("alertMessage", "Opis zadania został zdefiniowany");
        return "redirect:/dodaj_zadanie_do_listy/" + id;
    }

    @RequestMapping(value = "/dodaj_zadanie/{taskListId}/{taskDescriptionId}", method = RequestMethod.GET)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String addTask(@PathVariable("taskListId") Long taskListId, @PathVariable("taskDescriptionId") Long taskDescriptionId, Model model) {
        TaskDescription taskDescription = taskService.getTaskDescription(taskDescriptionId);
        TaskList taskList = taskService.getTaskList(taskListId);

        if (taskDescription == null || taskList == null)
            return "error_page";

        TaskForm taskForm = new TaskForm();
        taskForm.setTaskDescription(taskDescription);
        taskForm.setTaskList(taskList);
        taskForm.setName(taskDescription.getName());

        model.addAttribute("task", taskForm);
        return "forms/add_task";
    }

    @RequestMapping(value = "/dodaj_zadanie/{taskListId}/{taskDescriptionId}", method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String saveTask(@PathVariable("taskListId") Long taskListId, @PathVariable("taskDescriptionId") Long taskDescriptionId, @ModelAttribute("task") @Valid TaskForm taskForm, BindingResult bindingResult, Principal principal, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "redirect:/dodaj_zadanie/" + taskListId + "/" + taskDescriptionId;
        }

        TaskDescription taskDescription = taskService.getTaskDescription(taskDescriptionId);
        TaskList taskList = taskService.getTaskList(taskListId);

        if (taskDescription == null || taskList == null)
            return "error_page";

        taskForm.setTaskDescription(taskDescription);
        taskForm.setUser(userService.findByEmail(principal.getName()));
        taskForm.setTaskList(taskList);

        taskService.save(taskForm);

        redirectAttributes.addFlashAttribute("alertMessage", "Zadanie zostało dodane");

        return "redirect:/grupa/" + taskList.getGroup().getId();

    }

    @RequestMapping(value = "/dodaj_zadanie_do_listy/{taskListId}", method = RequestMethod.GET)
    @PreAuthorize("isFullyAuthenticated()")
    public String addTaskToTaskList(@PathVariable("taskListId") Long taskListId, Model model) {
        TaskList taskList = taskService.getTaskList(taskListId);

        if (taskList == null)
            return "error_page";

        Collection<TaskDescription> taskDescriptions = taskService.getTaskDescriptions();
        model.addAttribute("taskListId", taskListId);
        model.addAttribute("taskDescriptions", taskDescriptions);
        model.addAttribute("taskDescription", new TaskDescriptionForm());

        return "add_task_to_list";
    }


    @RequestMapping(value = "/zadanie/{id}", method = RequestMethod.GET)
    @PreAuthorize("isFullyAuthenticated()")
    public String showTask(@PathVariable("id") Long id, Model model, Principal principal) {
        Task task = taskService.getTask(id);

        if (task == null)
            return "error_page";


        User user = userService.findByEmail(principal.getName());
        Collection<Test> tests = taskService.getTests(task);
        Collection<Solution> solutions = taskService.getSolutions(user, task);
        Collection<Languages> languages = task.getLanguages();

        solutions = solutions.stream().sorted(Comparator.comparing(Solution::getDateOfSending).reversed()).collect(Collectors.toList());

        model.addAttribute("task", task);
        model.addAttribute("tests", tests);
        model.addAttribute("solutions", solutions);
        model.addAttribute("solutionForm", new SolutionForm());
        model.addAttribute("languages", languages);

        if (tests.isEmpty() || languages.isEmpty()) {
            model.addAttribute("alertMessage", "Nie zostały zdefiniowane żadne testy lub języki zadania nie zostały sprecyzowane.");
        }

        return "task";
    }

    @RequestMapping(value = "/wynik_testu/{id}", method = RequestMethod.GET)
    @PreAuthorize("isFullyAuthenticated()")
    public String showTestResult(@PathVariable("id") Long id, Model model) {
        Score score = taskService.getScore(id);
        if (score == null)
            return "error_page";

        model.addAttribute("log", score.getTestResult());
        model.addAttribute("title", "Wynik testu");

        return "log";
    }

    @RequestMapping(value = "/wyslij_zadanie/{id}", method = RequestMethod.POST)
    @PreAuthorize("isFullyAuthenticated()")
    public String saveSolution(@PathVariable("id") Long id, @ModelAttribute("solutionForm") @Valid SolutionForm solutionForm, BindingResult bindingResult, Principal principal, RedirectAttributes redirectAttributes) {
        Task task = taskService.getTask(id);
        if (bindingResult.hasErrors()) {
            return "redirect:/zadanie/" + task.getId();
        }

        solutionForm.setTask(task);
        solutionForm.setUser(userService.findByEmail(principal.getName()));
        solutionService.save(solutionForm);

        redirectAttributes.addFlashAttribute("alertMessage", "Zadanie zostało wysłane");
        return "redirect:/zadanie/" + task.getId();
    }


}