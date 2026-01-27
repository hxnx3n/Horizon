package config

import (
	"encoding/json"
	"os"
	"path/filepath"
	"runtime"
)

const authFileName = "horizon-agent.json"

type AuthConfig struct {
	Key        string `json:"key"`
	ServerURL  string `json:"serverUrl"`
	NodeID     string `json:"nodeId"`
	Registered bool   `json:"registered"`
}

func GetAuthConfigPath() string {
	if runtime.GOOS == "linux" {
		configDir := "/etc/horizon"
		if os.Geteuid() == 0 {
			os.MkdirAll(configDir, 0755)
		}
		if _, err := os.Stat(configDir); err == nil {
			return filepath.Join(configDir, authFileName)
		}
	}

	homeDir, err := os.UserHomeDir()
	if err != nil {
		return authFileName
	}

	configDir := filepath.Join(homeDir, ".config", "horizon")
	os.MkdirAll(configDir, 0700)
	return filepath.Join(configDir, authFileName)
}

func LoadAuthConfig() (*AuthConfig, error) {
	path := GetAuthConfigPath()

	data, err := os.ReadFile(path)
	if err != nil {
		if os.IsNotExist(err) {
			legacyPath := getLegacyPath()
			if legacyPath != "" {
				if data, err = os.ReadFile(legacyPath); err == nil {
					var config AuthConfig
					if err := json.Unmarshal(data, &config); err == nil {
						SaveAuthConfig(&config)
						os.Remove(legacyPath)
						return &config, nil
					}
				}
			}
			return nil, nil
		}
		return nil, err
	}

	var config AuthConfig
	if err := json.Unmarshal(data, &config); err != nil {
		return nil, err
	}

	return &config, nil
}

func getLegacyPath() string {
	homeDir, err := os.UserHomeDir()
	if err != nil {
		return ""
	}
	return filepath.Join(homeDir, ".horizon-agent-auth")
}

func SaveAuthConfig(config *AuthConfig) error {
	path := GetAuthConfigPath()

	dir := filepath.Dir(path)
	if err := os.MkdirAll(dir, 0755); err != nil {
		return err
	}

	data, err := json.MarshalIndent(config, "", "  ")
	if err != nil {
		return err
	}

	return os.WriteFile(path, data, 0600)
}

func DeleteAuthConfig() error {
	path := GetAuthConfigPath()
	err := os.Remove(path)
	if os.IsNotExist(err) {
		return nil
	}
	return err
}
