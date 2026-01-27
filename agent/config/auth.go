package config

import (
	"encoding/json"
	"os"
	"path/filepath"
)

const authFileName = ".horizon-agent-auth"

type AuthConfig struct {
	Key        string `json:"key"`
	ServerURL  string `json:"serverUrl"`
	Registered bool   `json:"registered"`
}

func GetAuthConfigPath() string {
	homeDir, err := os.UserHomeDir()
	if err != nil {
		return authFileName
	}
	return filepath.Join(homeDir, authFileName)
}

func LoadAuthConfig() (*AuthConfig, error) {
	path := GetAuthConfigPath()

	data, err := os.ReadFile(path)
	if err != nil {
		if os.IsNotExist(err) {
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

func SaveAuthConfig(config *AuthConfig) error {
	path := GetAuthConfigPath()

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
