require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name         = "RNInAppPurchase"
  s.version      = package['version']
  s.summary      = package['description']
  s.license      = package['license']

  s.authors      = package['author']
  s.homepage     = 'https://github.com/pedaling/react-native-in-app-purchase'
  s.platform     = :ios, "9.0"
  s.ios.deployment_target = '9.0'
  s.tvos.deployment_target = '10.0'

  s.source       = { :git => "https://github.com/pedaling/react-native-in-app-purchase.git", :tag => "v#{s.version}" }
  s.source_files  = "ios/**/*.{h,m,mm,swift}"

  s.dependency 'React'

  install_modules_dependencies(s)
end
