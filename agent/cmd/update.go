package cmd

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"os/exec"
	"runtime"
	"strings"
	"time"
)

var Version = "0.3.0"

const (
	githubRepo    = "hxnx3n/Horizon"
	binaryName    = "horizon-agent"
	releasePrefix = "agent-v"
)

type GitHubRelease struct {
	TagName string `json:"tag_name"`
	Assets  []struct {
		Name               string `json:"name"`
		BrowserDownloadURL string `json:"browser_download_url"`
	} `json:"assets"`
}

func RunVersion() {
	fmt.Printf("horizon-agent version %s\n", Version)
	fmt.Printf("  OS/Arch: %s/%s\n", runtime.GOOS, runtime.GOARCH)
}

func RunUpdate(targetVersion string) error {
	fmt.Println("Checking for updates...")

	var release *GitHubRelease
	var err error

	if targetVersion != "" {
		fmt.Printf("Searching for version %s...\n", targetVersion)
		release, err = getReleaseByVersion(targetVersion)
		if err != nil {
			return fmt.Errorf("failed to find version %s: %w", targetVersion, err)
		}
	} else {
		release, err = getLatestRelease()
		if err != nil {
			return fmt.Errorf("failed to check for updates: %w", err)
		}
	}

	latestVersion := strings.TrimPrefix(release.TagName, releasePrefix)
	currentVersion := strings.TrimPrefix(Version, "v")

	fmt.Printf("  Current version: %s\n", Version)
	fmt.Printf("  Target version:  %s\n", latestVersion)

	if targetVersion == "" && Version != "dev" && currentVersion == latestVersion {
		fmt.Println("\n✓ You are already running the latest version.")
		return nil
	}

	binaryFileName := getBinaryFileName()
	var downloadURL string

	for _, asset := range release.Assets {
		if asset.Name == binaryFileName {
			downloadURL = asset.BrowserDownloadURL
			break
		}
	}

	if downloadURL == "" {
		return fmt.Errorf("no binary available for %s/%s", runtime.GOOS, runtime.GOARCH)
	}

	fmt.Printf("\nDownloading %s...\n", binaryFileName)

	tempFile, err := downloadBinary(downloadURL)
	if err != nil {
		return fmt.Errorf("failed to download update: %w", err)
	}
	defer os.Remove(tempFile)

	execPath, err := os.Executable()
	if err != nil {
		return fmt.Errorf("failed to get executable path: %w", err)
	}

	if err := replaceBinary(tempFile, execPath); err != nil {
		return fmt.Errorf("failed to install update: %w", err)
	}

	fmt.Printf("\n✓ Successfully updated to version %s\n", latestVersion)
	fmt.Println("  Please restart the agent if it's running.")

	return nil
}

func getLatestRelease() (*GitHubRelease, error) {
	client := &http.Client{Timeout: 30 * time.Second}

	req, err := http.NewRequest("GET", fmt.Sprintf("https://api.github.com/repos/%s/releases", githubRepo), nil)
	if err != nil {
		return nil, err
	}
	req.Header.Set("Accept", "application/vnd.github.v3+json")

	resp, err := client.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("GitHub API returned status %d", resp.StatusCode)
	}

	var releases []GitHubRelease
	if err := json.NewDecoder(resp.Body).Decode(&releases); err != nil {
		return nil, err
	}

	for _, release := range releases {
		if strings.HasPrefix(release.TagName, releasePrefix) {
			return &release, nil
		}
	}

	return nil, fmt.Errorf("no agent releases found")
}

func getReleaseByVersion(version string) (*GitHubRelease, error) {
	client := &http.Client{Timeout: 30 * time.Second}

	req, err := http.NewRequest("GET", fmt.Sprintf("https://api.github.com/repos/%s/releases", githubRepo), nil)
	if err != nil {
		return nil, err
	}
	req.Header.Set("Accept", "application/vnd.github.v3+json")

	resp, err := client.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("GitHub API returned status %d", resp.StatusCode)
	}

	var releases []GitHubRelease
	if err := json.NewDecoder(resp.Body).Decode(&releases); err != nil {
		return nil, err
	}

	normalizedVersion := strings.TrimPrefix(version, "v")

	for _, release := range releases {
		if strings.HasPrefix(release.TagName, releasePrefix) {
			releaseVersion := strings.TrimPrefix(release.TagName, releasePrefix)
			if releaseVersion == normalizedVersion {
				return &release, nil
			}
		}
	}

	return nil, fmt.Errorf("version %s not found", version)
}

func getBinaryFileName() string {
	ext := ""
	if runtime.GOOS == "windows" {
		ext = ".exe"
	}
	return fmt.Sprintf("%s-%s-%s%s", binaryName, runtime.GOOS, runtime.GOARCH, ext)
}

func downloadBinary(url string) (string, error) {
	client := &http.Client{Timeout: 5 * time.Minute}

	resp, err := client.Get(url)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return "", fmt.Errorf("download failed with status %d", resp.StatusCode)
	}

	tempFile, err := os.CreateTemp("", "horizon-agent-update-*")
	if err != nil {
		return "", err
	}
	defer tempFile.Close()

	_, err = io.Copy(tempFile, resp.Body)
	if err != nil {
		os.Remove(tempFile.Name())
		return "", err
	}

	return tempFile.Name(), nil
}

func replaceBinary(newBinary, targetPath string) error {
	if err := os.Chmod(newBinary, 0755); err != nil {
		return err
	}

	if runtime.GOOS == "windows" {
		oldPath := targetPath + ".old"
		if err := os.Rename(targetPath, oldPath); err != nil {
			return err
		}
		if err := copyFile(newBinary, targetPath); err != nil {
			os.Rename(oldPath, targetPath)
			return err
		}
		go func() {
			time.Sleep(time.Second)
			os.Remove(oldPath)
		}()
	} else {
		if strings.HasPrefix(targetPath, "/usr/local/bin") {
			cmd := exec.Command("sudo", "mv", newBinary, targetPath)
			cmd.Stdout = os.Stdout
			cmd.Stderr = os.Stderr
			cmd.Stdin = os.Stdin
			if err := cmd.Run(); err != nil {
				return err
			}
			cmd = exec.Command("sudo", "chmod", "+x", targetPath)
			cmd.Run()
		} else {
			if err := os.Rename(newBinary, targetPath); err != nil {
				return copyFile(newBinary, targetPath)
			}
		}
	}

	return nil
}

func copyFile(src, dst string) error {
	sourceFile, err := os.Open(src)
	if err != nil {
		return err
	}
	defer sourceFile.Close()

	destFile, err := os.Create(dst)
	if err != nil {
		return err
	}
	defer destFile.Close()

	_, err = io.Copy(destFile, sourceFile)
	if err != nil {
		return err
	}

	return os.Chmod(dst, 0755)
}
